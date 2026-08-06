package io.github.gjuton.internal.parser;

import static java.util.Map.entry;

import io.github.gjuton.internal.model.UntypedSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites a JSON Schema tree into the subset of shapes the schema model
 * can be deserialised from, so that no information is lost to
 * {@link UntypedSchema} or to Jackson's type dispatch.
 *
 * <p>Normalisation is idempotent: a subtree may safely be normalised again
 * after the document containing it already has been.
 *
 * <p>Only recurses into positions that hold sub-schemas ({@code properties}
 * values, {@code items}, {@code oneOf}/{@code anyOf}/{@code allOf}
 * elements, etc.) — never into {@code const}/{@code enum} payloads or
 * object keys, which may coincidentally share a name with a schema
 * keyword (e.g. a property named {@code "pattern"}).
 */
final class SchemaNormalizer {

    /**
     * Describes how a JSON Schema keyword's value relates to sub-schemas,
     * so the walker knows how to recurse into it.
     */
    enum SchemaShape {
        /** The value is a single sub-schema (e.g. {@code if}, {@code not}). */
        SCHEMA,
        /** The value is an array of sub-schemas (e.g. {@code oneOf}). */
        SCHEMA_ARRAY,
        /**
         * The value is an object whose values are sub-schemas (e.g. {@code properties}).
         * An entry may hold data instead — a Draft 7 {@code dependencies} entry is
         * either a sub-schema or a property-name array — which walkers skip.
         */
        SCHEMA_MAP,
        /** The value is either a single sub-schema or an array of sub-schemas (e.g. {@code items}). */
        SCHEMA_OR_SCHEMA_ARRAY
    }

    private static final List<String> OBJECT_KEYWORDS = List.of(
            "properties", "required", "additionalProperties",
            "minProperties", "maxProperties", "dependencies",
            "dependentRequired", "dependentSchemas",
            "patternProperties", "propertyNames");

    private static final List<String> STRING_KEYWORDS = List.of(
            "pattern", "minLength", "maxLength", "format");

    private static final List<String> NUMBER_KEYWORDS = List.of(
            "minimum", "maximum", "exclusiveMinimum",
            "exclusiveMaximum", "multipleOf");

    private static final List<String> ARRAY_KEYWORDS = List.of(
            "items", "prefixItems", "additionalItems",
            "contains", "minItems", "maxItems", "uniqueItems");

    /**
     * The JSON Schema keywords whose values hold sub-schemas, and the shape
     * each one takes. Any keyword absent from this map holds data, not a
     * schema.
     */
    static final Map<String, SchemaShape> SCHEMA_FIELDS = Map.ofEntries(
            entry("properties", SchemaShape.SCHEMA_MAP),
            entry("definitions", SchemaShape.SCHEMA_MAP),
            entry("$defs", SchemaShape.SCHEMA_MAP),
            entry("dependentSchemas", SchemaShape.SCHEMA_MAP),
            entry("dependencies", SchemaShape.SCHEMA_MAP),
            entry("patternProperties", SchemaShape.SCHEMA_MAP),
            entry("oneOf", SchemaShape.SCHEMA_ARRAY),
            entry("anyOf", SchemaShape.SCHEMA_ARRAY),
            entry("allOf", SchemaShape.SCHEMA_ARRAY),
            entry("prefixItems", SchemaShape.SCHEMA_ARRAY),
            entry("items", SchemaShape.SCHEMA_OR_SCHEMA_ARRAY),
            entry("additionalItems", SchemaShape.SCHEMA),
            entry("contains", SchemaShape.SCHEMA),
            entry("additionalProperties", SchemaShape.SCHEMA),
            entry("if", SchemaShape.SCHEMA),
            entry("then", SchemaShape.SCHEMA),
            entry("else", SchemaShape.SCHEMA),
            entry("not", SchemaShape.SCHEMA),
            entry("propertyNames", SchemaShape.SCHEMA)
    );

    private SchemaNormalizer() {
    }

    /**
     * A copy of {@code node} that shares no mutable structure with it, so
     * either may be changed without the other seeing it.
     */
    private static Object deepCopy(Object node) {
        if (node instanceof Map<?, ?> objectNode) {
            var copy = new LinkedHashMap<String, Object>();
            for (var entry : objectNode.entrySet()) {
                var copiedValue = deepCopy(entry.getValue());
                copy.put((String) entry.getKey(), copiedValue);
            }
            return copy;
        }
        if (node instanceof List<?> arrayNode) {
            var copy = new ArrayList<>();
            for (var element : arrayNode) {
                var copiedElement = deepCopy(element);
                copy.add(copiedElement);
            }
            return copy;
        }
        // Scalars are immutable, so sharing one is indistinguishable from copying it.
        return node;
    }

    /**
     * Normalises {@code node} and every sub-schema beneath it in place.
     *
     * <p>Afterwards no schema declares an array of types, and every schema
     * whose keywords imply exactly one type declares that type explicitly.
     * A node that is already normalised is left unchanged. A value that is
     * not a JSON object — including one naming nothing at all — is left
     * alone rather than rejected.
     */
    static void normalize(Object node) {
        // Type arrays first: inference reads the type keyword, and the branches
        // this produces are themselves schemas that still need inferring.
        rewriteTypeArrays(node);
        inferMissingTypes(node);
    }

    /**
     * Walks the given JSON tree and adds a {@code "type"} field to every
     * object node that (a) lacks one and (b) contains keywords implying
     * exactly one JSON Schema type.
     */
    private static void inferMissingTypes(Object node) {
        if (!(node instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        var objectNode = (Map<String, Object>) node;
        if (!objectNode.containsKey("type")) {
            var inferred = inferType(objectNode);
            if (inferred != null) {
                objectNode.put("type", inferred);
            }
        }
        for (var field : SCHEMA_FIELDS.entrySet()) {
            var value = objectNode.get(field.getKey());
            if (value == null) {
                continue;
            }
            switch (field.getValue()) {
                case SCHEMA -> inferMissingTypes(value);
                case SCHEMA_ARRAY -> {
                    if (value instanceof List<?> arrayValue) {
                        for (var element : arrayValue) {
                            inferMissingTypes(element);
                        }
                    }
                }
                case SCHEMA_MAP -> {
                    if (value instanceof Map<?, ?> mapValue) {
                        for (var entry : mapValue.entrySet()) {
                            inferMissingTypes(entry.getValue());
                        }
                    }
                }
                case SCHEMA_OR_SCHEMA_ARRAY -> {
                    if (value instanceof List<?> arrayValue) {
                        for (var element : arrayValue) {
                            inferMissingTypes(element);
                        }
                    } else {
                        inferMissingTypes(value);
                    }
                }
                default -> throw new IllegalStateException("Unhandled field type: " + field.getValue());
            }
        }
    }

    /**
     * Returns the single type implied by {@code node}'s present keywords,
     * or {@code null} if no type-specific keyword is present or keywords
     * from more than one type are mixed.
     */
    private static String inferType(Map<String, Object> node) {
        var candidates = new ArrayList<String>(4);
        if (hasAny(node, OBJECT_KEYWORDS)) {
            candidates.add("object");
        }
        if (hasAny(node, STRING_KEYWORDS)) {
            candidates.add("string");
        }
        if (hasAny(node, NUMBER_KEYWORDS)) {
            candidates.add("number");
        }
        if (hasAny(node, ARRAY_KEYWORDS)) {
            candidates.add("array");
        }
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    private static boolean hasAny(Map<String, Object> node, List<String> keywords) {
        return keywords.stream().anyMatch(node::containsKey);
    }

    /**
     * Normalises the Draft 7 {@code "type": ["string", "null"]} shorthand
     * into an explicit {@code oneOf} before deserialisation. Jackson uses
     * the scalar {@code type} field for subclass dispatch, so the array
     * form must be rewritten before deserialisation can succeed.
     *
     * <p>For example,
     * <pre>{@code
     * {
     *     "type": ["string", "null"],
     *     "minLength": 3
     * }
     * }</pre>
     * becomes
     * <pre>{@code
     * {
     *     "oneOf": [
     *         {"type": "string", "minLength": 3},
     *         {"type": "null", "minLength": 3}
     *     ]
     * }
     * }</pre>
     *
     * <p>All properties from the original node are copied into each branch;
     * constraints irrelevant to a given type are silently ignored during
     * deserialisation.
     */
    private static void rewriteTypeArrays(Object node) {
        if (node instanceof Map) {
            @SuppressWarnings("unchecked")
            var objectNode = (Map<String, Object>) node;
            var typeNode = objectNode.get("type");
            if (typeNode instanceof List<?> typeArray) {
                var oneOfArray = new ArrayList<>();
                for (var typeElement : typeArray) {
                    @SuppressWarnings("unchecked")
                    var branch = (Map<String, Object>) deepCopy(objectNode);
                    branch.put("type", typeElement);
                    oneOfArray.add(branch);
                }
                var definitions = objectNode.get("definitions");
                var defs = objectNode.get("$defs");
                objectNode.clear();
                if (definitions != null) {
                    objectNode.put("definitions", definitions);
                }
                if (defs != null) {
                    objectNode.put("$defs", defs);
                }
                objectNode.put("oneOf", oneOfArray);
            }
            // The rewrite above replaces this node's own entries, so the walk reads a
            // snapshot of them rather than a live view of the map it just repopulated.
            var values = new ArrayList<>(objectNode.values());
            for (var value : values) {
                rewriteTypeArrays(value);
            }
        } else if (node instanceof List<?> arrayNode) {
            for (var element : arrayNode) {
                rewriteTypeArrays(element);
            }
        }
    }
}
