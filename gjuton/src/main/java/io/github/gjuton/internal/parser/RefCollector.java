package io.github.gjuton.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gjuton.internal.model.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves every {@code $ref} reachable from a JSON Schema document into
 * the schema it names.
 *
 * <p>A {@code $ref} may name a position within the document it appears in
 * or one in a separate document, identified by a relative path or an
 * HTTP(S) URL. Both are resolved; refs that form a cycle resolve to the
 * schema they point at rather than looping.
 */
final class RefCollector {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path baseDir;

    private RefCollector(Path baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Resolves every {@code $ref} reachable from {@code rootNode}, keyed by
     * the ref string exactly as it appears in the document. Refs naming the
     * document itself map to {@code rootSchema}, which must be the schema
     * {@code rootNode} was deserialised into.
     *
     * <p>Relative refs to external documents resolve against {@code baseDir};
     * when it is {@code null} the document has no location and such a ref
     * cannot be resolved.
     *
     * @throws IllegalArgumentException if a {@code $ref} names something that
     *     is not a schema, or cannot be resolved at all
     */
    static Map<String, Schema> collect(JsonNode rootNode, Schema rootSchema, Path baseDir) throws JsonProcessingException {
        var refs = new HashMap<String, Schema>();
        // Self-reference always resolves to the same root Schema instance so phase state
        // is shared between the root and any "#" ref.
        refs.put("#", rootSchema);
        var collector = new RefCollector(baseDir);
        collector.walk(rootNode, rootNode, null, refs);
        return refs;
    }

    /**
     * Resolves every {@code $ref} at or beneath {@code node} into
     * {@code refs}, treating {@code currentDoc} as the document those refs
     * are relative to. {@code currentDocUri} is the URI {@code currentDoc}
     * was loaded from, or {@code null} for the document parsing started at.
     *
     * <p>Refs already present in {@code refs} are left as they are, which is
     * what stops a cycle from recursing forever.
     */
    private void walk(JsonNode node, JsonNode currentDoc, String currentDocUri, Map<String, Schema> refs) throws JsonProcessingException {
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
                    var targetDoc = currentDoc;
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
                        targetDoc = loadExternalDocument(baseUri);
                        targetDocUri = baseUri;
                    }
                    // The target may sit outside any sub-schema position and so be
                    // reachable only by following the ref that points at it. An empty
                    // fragment names the document itself.
                    var target = targetDoc.at(fragment);
                    // Out of reach of the document-wide pass for that same reason, so the
                    // target is normalised here, where following the ref has established
                    // it is a schema.
                    SchemaNormalizer.normalize(target);
                    // Recording the ref before walking on is what terminates a cycle.
                    var targetSchema = resolveFragment(fragment, targetDoc);
                    refs.put(ref, targetSchema);
                    walk(target, targetDoc, targetDocUri, refs);
                }
            }
            for (var property : node.properties()) {
                var shape = SchemaNormalizer.SCHEMA_FIELDS.get(property.getKey());
                if (shape == null) {
                    continue;
                }
                var value = property.getValue();
                if (shape == SchemaNormalizer.SchemaShape.SCHEMA_MAP) {
                    // The keys of a schema map are user-chosen property or definition
                    // names, so its schemas sit one level below the keyword.
                    for (var entry : value.properties()) {
                        walk(entry.getValue(), currentDoc, currentDocUri, refs);
                    }
                } else {
                    walk(value, currentDoc, currentDocUri, refs);
                }
            }
        } else if (node.isArray()) {
            // Only reachable from a whitelisted keyword above, so an array of
            // schemas is walked while an enum payload is never entered.
            for (var element : node) {
                walk(element, currentDoc, currentDocUri, refs);
            }
        }
    }

    /**
     * Deserialises the schema at the given JSON Pointer within
     * {@code document}. An empty pointer names the document itself.
     *
     * @throws IllegalArgumentException if the pointer names nothing, or
     *     names something that is not a schema
     */
    private static Schema resolveFragment(String pointer, JsonNode document) throws JsonProcessingException {
        if (pointer.isEmpty()) {
            return MAPPER.treeToValue(document, Schema.class);
        }
        var target = document.at(pointer);
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
     * Loads and normalises the external JSON Schema document at
     * {@code uri}, which is either an HTTP(S) URL or a path relative to the
     * document that referenced it. Refs within the returned document are
     * qualified with {@code uri}, so they stay distinct from same-named refs
     * in other documents.
     *
     * @throws IllegalArgumentException if {@code uri} is relative and the
     *     referencing document has no location to resolve it against
     * @throws java.io.UncheckedIOException if the document cannot be read
     */
    private JsonNode loadExternalDocument(String uri) {
        try {
            JsonNode document;
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                document = MAPPER.readTree(SchemaFetcher.fetch(uri));
            } else if (baseDir != null) {
                document = MAPPER.readTree(Files.readString(baseDir.resolve(uri)));
            } else {
                throw new IllegalArgumentException(
                        "Cannot resolve relative $ref '" + uri
                                + "': no base URI. Use SchemaParser.parse(Path) to parse from a file.");
            }
            SchemaNormalizer.normalize(document);
            qualifyRefs(document, uri);
            return document;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Rewrites document-local {@code $ref} values ({@code "#/definitions/Foo"})
     * within {@code document} to name {@code baseUri} explicitly
     * ({@code "baseUri#/definitions/Foo"}). Must run before {@code document}
     * is deserialised, so the resulting schemas carry ref strings that match
     * the keys they are collected under.
     */
    private static void qualifyRefs(JsonNode node, String baseUri) {
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
                qualifyRefs(entry.getValue(), baseUri);
            }
        } else if (node.isArray()) {
            for (var element : node) {
                qualifyRefs(element, baseUri);
            }
        }
    }
}
