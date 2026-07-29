package io.github.gjuton.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.SchemaDocument;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses a JSON Schema document into the internal model. Supports the
 * most common keywords across drafts rather than strict compliance with
 * any single draft version.
 */
public final class SchemaParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Parses a JSON Schema string into a {@link SchemaDocument} containing
     * the root schema and all resolved {@code $ref} targets.
     *
     * @throws IllegalArgumentException if the input is not valid JSON or
     *     contains an unresolvable {@code $ref}
     */
    public static SchemaDocument parse(String jsonSchema) {
        return doParse(jsonSchema, null);
    }

    /**
     * Parses a JSON Schema file into a {@link SchemaDocument} containing
     * the root schema and all resolved {@code $ref} targets. External
     * {@code $ref} values are resolved relative to the file's parent directory.
     *
     * @throws IllegalArgumentException if the schema is not valid JSON or
     *     contains an unresolvable {@code $ref}
     * @throws UncheckedIOException if reading the file fails
     */
    public static SchemaDocument parse(Path schemaFile) {
        try {
            var jsonSchema = Files.readString(schemaFile);
            return doParse(jsonSchema, schemaFile.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static SchemaDocument doParse(String jsonSchema, Path baseDir) {
        try {
            var rootNode = MAPPER.readTree(jsonSchema);
            rewriteTypeArrays(rootNode);
            TypeInferrer.inferMissingTypes(rootNode);
            var rootSchema = MAPPER.treeToValue(rootNode, Schema.class);
            var refs = new HashMap<String, Schema>();
            // Self-reference always resolves to the same root Schema instance so phase state
            // is shared between the root and any "#" ref.
            refs.put("#", rootSchema);
            collectRefs(rootNode, rootNode, refs, baseDir);
            return new SchemaDocument(rootSchema, refs);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON Schema", e);
        }
    }

    /**
     * Normalises the Draft 7 {@code "type": ["string", "null"]} shorthand
     * into an explicit {@code oneOf} before deserialisation. Jackson uses
     * the scalar {@code type} field for subclass dispatch, so the array
     * form must be rewritten before {@code treeToValue} can succeed.
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
    private static void rewriteTypeArrays(JsonNode node) {
        if (node.isObject()) {
            var objectNode = (ObjectNode) node;
            var typeNode = objectNode.get("type");
            if (typeNode != null && typeNode.isArray()) {
                var oneOfArray = MAPPER.createArrayNode();
                for (var typeElement : typeNode) {
                    var branch = objectNode.deepCopy();
                    branch.set("type", typeElement);
                    oneOfArray.add(branch);
                }
                var definitions = objectNode.get("definitions");
                var defs = objectNode.get("$defs");
                objectNode.removeAll();
                if (definitions != null) {
                    objectNode.set("definitions", definitions);
                }
                if (defs != null) {
                    objectNode.set("$defs", defs);
                }
                objectNode.set("oneOf", oneOfArray);
            }
            for (var entry : objectNode.properties()) {
                rewriteTypeArrays(entry.getValue());
            }
        } else if (node.isArray()) {
            for (var element : node) {
                rewriteTypeArrays(element);
            }
        }
    }

    private static void collectRefs(
            JsonNode node, JsonNode root, Map<String, Schema> refs,
            Path baseDir) throws JsonProcessingException {
        collectRefs(node, root, refs, baseDir, null);
    }

    private static void collectRefs(
            JsonNode node, JsonNode root, Map<String, Schema> refs,
            Path baseDir, String currentDocUri) throws JsonProcessingException {
        if (node.isObject()) {
            var refNode = node.get("$ref");
            if (refNode != null && refNode.isTextual()) {
                var ref = refNode.asText();
                if (!refs.containsKey(ref)) {
                    // Split the ref into the document it names and the fragment within it:
                    // "defs.json#/definitions/Address" sets targetDoc to the loaded defs.json,
                    // targetDocUri to "defs.json" and fragment to "/definitions/Address", while
                    // "#/definitions/Address" leaves targetDoc and targetDocUri as the current
                    // document and sets fragment to "/definitions/Address".
                    var targetDoc = root;
                    var targetDocUri = currentDocUri;
                    String fragment;
                    if (ref.startsWith("#")) {
                        fragment = ref.substring(1);
                    } else if (currentDocUri != null && ref.startsWith(currentDocUri + "#")) {
                        fragment = ref.substring(ref.indexOf('#') + 1);
                    } else {
                        int fragIdx = ref.indexOf('#');
                        var baseUri = fragIdx >= 0 ? ref.substring(0, fragIdx) : ref;
                        fragment = fragIdx >= 0 ? ref.substring(fragIdx + 1) : "";
                        targetDoc = loadExternalDocument(baseUri, baseDir);
                        qualifyRefsWithinExternalDocument(targetDoc, baseUri);
                        targetDocUri = baseUri;
                    }
                    // Recording the ref before walking on is what terminates a cycle.
                    var targetSchema = resolveFragment(fragment, targetDoc);
                    refs.put(ref, targetSchema);
                    // The target may sit outside any sub-schema position and so be
                    // reachable only by following the ref that points at it. An empty
                    // fragment names the document itself.
                    var target = targetDoc.at(fragment);
                    collectRefs(target, targetDoc, refs, baseDir, targetDocUri);
                }
            }
            for (var property : node.properties()) {
                var shape = TypeInferrer.SCHEMA_FIELDS.get(property.getKey());
                if (shape == null) {
                    continue;
                }
                var value = property.getValue();
                if (shape == TypeInferrer.SchemaShape.SCHEMA_MAP) {
                    // The keys of a schema map are user-chosen property or definition
                    // names, so its schemas sit one level below the keyword.
                    for (var entry : value.properties()) {
                        collectRefs(entry.getValue(), root, refs, baseDir, currentDocUri);
                    }
                } else {
                    collectRefs(value, root, refs, baseDir, currentDocUri);
                }
            }
        } else if (node.isArray()) {
            // Only reachable from a whitelisted keyword above, so an array of
            // schemas is walked while an enum payload is never entered.
            for (var element : node) {
                collectRefs(element, root, refs, baseDir, currentDocUri);
            }
        }
    }

    private static Schema resolveFragment(String pointer, JsonNode root) throws JsonProcessingException {
        if (pointer.isEmpty()) {
            return MAPPER.treeToValue(root, Schema.class);
        }
        var target = root.at(pointer);
        if (target.isMissingNode()) {
            throw new IllegalArgumentException("Unresolved $ref fragment: #" + pointer);
        }
        var schema = MAPPER.treeToValue(target, Schema.class);
        if (schema == null) {
            throw new IllegalArgumentException("$ref target is not a schema: #" + pointer);
        }
        return schema;
    }

    /**
     * Loads an external JSON Schema document by URI. Relative URIs resolve
     * against {@code baseDir}; HTTP(S) URIs are fetched over the network.
     */
    private static JsonNode loadExternalDocument(String uri, Path baseDir) {
        try {
            JsonNode node;
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                node = MAPPER.readTree(SchemaFetcher.fetch(uri));
            } else if (baseDir != null) {
                node = MAPPER.readTree(Files.readString(baseDir.resolve(uri)));
            } else {
                throw new IllegalArgumentException(
                        "Cannot resolve relative $ref '" + uri
                                + "': no base URI. Use SchemaParser.parse(Path) to parse from a file.");
            }
            rewriteTypeArrays(node);
            TypeInferrer.inferMissingTypes(node);
            return node;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Rewrites {@code $ref} values in an external document from the
     * document-local form ({@code "#/definitions/Foo"}) to fully-qualified
     * form ({@code "baseUri#/definitions/Foo"}). This must run before any
     * deserialization so that the resulting {@link Schema} objects carry
     * qualified ref strings that match the keys in the shared refs map.
     */
    private static void qualifyRefsWithinExternalDocument(JsonNode node, String baseUri) {
        if (node.isObject()) {
            var objectNode = (ObjectNode) node;
            var refNode = objectNode.get("$ref");
            if (refNode != null && refNode.isTextual()) {
                var ref = refNode.asText();
                if (ref.startsWith("#")) {
                    objectNode.put("$ref", baseUri + ref);
                }
            }
            for (var entry : objectNode.properties()) {
                qualifyRefsWithinExternalDocument(entry.getValue(), baseUri);
            }
        } else if (node.isArray()) {
            for (var element : node) {
                qualifyRefsWithinExternalDocument(element, baseUri);
            }
        }
    }
}
