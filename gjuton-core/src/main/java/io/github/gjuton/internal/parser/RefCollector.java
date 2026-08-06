package io.github.gjuton.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.gjuton.internal.model.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
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

    private RefCollector() {
    }

    /**
     * Resolves every {@code $ref} reachable from {@code rootNode} into the
     * schema it names. A ref written in {@code rootNode} is keyed as written;
     * one inside an external document is keyed by its absolute URI. Either
     * way the key matches the ref string the deserialised schemas carry.
     * Refs naming the document itself map to {@code rootSchema}, which must
     * be the schema {@code rootNode} was deserialised into.
     *
     * <p>{@code retrievalUri} is where {@code rootNode} was loaded from, or
     * {@code null} when it came from nowhere on disk or the network. A
     * document that states no identity of its own needs it to resolve a
     * relative {@code $ref}.
     *
     * @throws IllegalArgumentException if a {@code $ref} names something that
     *     is not a schema, or cannot be resolved at all
     */
    static Map<String, Schema> collect(JsonNode rootNode, Schema rootSchema, URI retrievalUri) throws JsonProcessingException {
        var refs = new HashMap<String, Schema>();
        // Self-reference always resolves to the same root Schema instance so phase state
        // is shared between the root and any "#" ref.
        refs.put("#", rootSchema);
        // The root's own $id is applied by the walk, so the retrieval URI is what
        // encloses it. Naming that identity is a second way to refer to the document
        // itself, besides "#".
        var entryIdentity = baseUriOf(rootNode, retrievalUri);
        var entryDocUri = entryIdentity != null ? entryIdentity.toString() : null;
        walk(rootNode, rootNode, entryDocUri, retrievalUri, refs, new HashMap<>());
        return refs;
    }

    /**
     * Resolves every {@code $ref} at or beneath {@code node} into
     * {@code refs}, treating {@code currentDoc} as the document those refs
     * are relative to. {@code currentDocUri} is how a {@code $ref} may name
     * {@code currentDoc} — the identity it states, or where it was retrieved
     * from — and is {@code null} when it has neither.
     * {@code enclosingBase} is the identity of the scope {@code node} sits in.
     *
     * <p>Refs already present in {@code refs} are left as they are, which is
     * what stops a cycle from recursing forever.
     */
    private static void walk(JsonNode node, JsonNode currentDoc, String currentDocUri, URI enclosingBase, Map<String, Schema> refs,
            Map<URI, JsonNode> loadedDocuments) throws JsonProcessingException {
        if (node.isObject()) {
            var baseUri = baseUriOf(node, enclosingBase);
            var refNode = node.get("$ref");
            if (refNode != null && refNode.isTextual()) {
                var ref = refNode.asText();
                if (!refs.containsKey(ref)) {
                    // Split the ref into the document it names and the fragment within it:
                    // "defs.json#/definitions/Address" sets targetDoc to the loaded defs.json,
                    // targetDocUri to what that document is called and fragment to
                    // "/definitions/Address", while "#/definitions/Address" leaves targetDoc
                    // and targetDocUri as the current document and sets the same fragment.
                    var targetDoc = currentDoc;
                    var targetDocUri = currentDocUri;
                    var targetBase = baseUri;
                    String fragment;
                    if (ref.startsWith("#")) {
                        fragment = ref.substring(1);
                    } else if (currentDocUri != null && (ref.equals(currentDocUri) || ref.startsWith(currentDocUri + "#"))) {
                        // Spelled out in full, but naming the document already in hand.
                        int fragIdx = ref.indexOf('#');
                        fragment = fragIdx >= 0 ? ref.substring(fragIdx + 1) : "";
                    } else {
                        // A ref naming a separate document: what precedes the "#" says which
                        // document, what follows says where within it. Either part may be
                        // absent — "defs.json" names a whole document and leaves the fragment
                        // empty. Which document that spelling names depends on where the ref
                        // was written, so it is read against the base URI in force here.
                        int fragIdx = ref.indexOf('#');
                        var documentPart = fragIdx >= 0 ? ref.substring(0, fragIdx) : ref;
                        fragment = fragIdx >= 0 ? ref.substring(fragIdx + 1) : "";
                        var location = baseUri != null ? baseUri.resolve(documentPart) : URI.create(documentPart);
                        if (!location.isAbsolute()) {
                            // No base URI: the document came from a string or stream and
                            // declares no $id, so a relative ref names nothing reachable.
                            throw new IllegalArgumentException(
                                    "Cannot resolve relative $ref '" + documentPart
                                            + "': no base URI. Use SchemaParser.parse(Path) to parse from a file.");
                        }
                        targetDoc = loadExternalDocument(location, loadedDocuments);
                        // A document is known by the identity it states, or by where it was
                        // retrieved from when it states none — the one name it has however it
                        // was reached, and the base its own refs are read against.
                        targetBase = baseUriOf(targetDoc, location);
                        targetDocUri = targetBase.toString();
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
                    walk(target, targetDoc, targetDocUri, targetBase, refs, loadedDocuments);
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
                        walk(entry.getValue(), currentDoc, currentDocUri, baseUri, refs, loadedDocuments);
                    }
                } else {
                    walk(value, currentDoc, currentDocUri, baseUri, refs, loadedDocuments);
                }
            }
        } else if (node.isArray()) {
            // Only reachable from a whitelisted keyword above, so an array of
            // schemas is walked while an enum payload is never entered.
            for (var element : node) {
                walk(element, currentDoc, currentDocUri, enclosingBase, refs, loadedDocuments);
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
     * Returns the identity {@code node} carries: the one it states with
     * {@code $id}, or with {@code id} as Draft 4 spells the same keyword,
     * understood relative to the scope it sits in. A node stating none
     * shares the identity of that scope, which may be unknown.
     *
     * <p>A node stating {@code "$id": "external/billing.json"} within a scope
     * identified as {@code file:/schemas/root.json} is identified as
     * {@code file:/schemas/external/billing.json} — the last segment is
     * replaced, not appended to. An absolute {@code $id} stands on its own,
     * whatever scope it sits in.
     */
    private static URI baseUriOf(JsonNode node, URI enclosingBase) {
        if (!node.isObject()) {
            return enclosingBase;
        }
        var idNode = node.get("$id");
        if (idNode == null) {
            idNode = node.get("id");
        }
        if (idNode == null || !idNode.isTextual()) {
            return enclosingBase;
        }
        var declaredId = URI.create(idNode.asText());
        return enclosingBase != null ? enclosingBase.resolve(declaredId) : declaredId;
    }

    /**
     * Returns the JSON Schema document that {@code location} identifies, with
     * every ref it contains restated as the absolute URI that ref names.
     *
     * <p>A document is retrieved once per parse: {@code loadedDocuments} holds
     * those already reached, and gains any this call retrieves.
     *
     * @throws java.io.UncheckedIOException if the document cannot be read
     */
    private static JsonNode loadExternalDocument(URI location, Map<URI, JsonNode> loadedDocuments) {
        var alreadyLoaded = loadedDocuments.get(location);
        if (alreadyLoaded != null) {
            return alreadyLoaded;
        }
        try {
            String content;
            var scheme = location.getScheme();
            if ("http".equals(scheme) || "https".equals(scheme)) {
                var url = location.toString();
                content = SchemaFetcher.fetch(url);
            } else {
                var path = Path.of(location);
                content = Files.readString(path);
            }
            var document = MAPPER.readTree(content);
            SchemaNormalizer.normalize(document);
            var identity = baseUriOf(document, location);
            qualifyRefs(document, identity.toString());
            loadedDocuments.put(location, document);
            return document;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Restates every relative {@code $ref} within {@code document} as the
     * absolute URI it names, resolved against {@code docUri} — what
     * {@code document} itself is called. Absolute refs are left alone.
     *
     * <p>Same-named refs in different documents therefore stay distinct, and
     * a document reached by more than one route is named once however it was
     * reached. Must run before {@code document} is deserialised,
     * so the resulting schemas carry ref strings matching the keys they are
     * collected under.
     *
     * <p>Only refs in schema positions are restated. A {@code $ref}-shaped
     * value sitting in data — an {@code enum} member, a {@code const}, an
     * {@code example} — means nothing as a reference and is left as the data
     * it is.
     */
    private static void qualifyRefs(JsonNode node, String docUri) {
        if (node.isObject()) {
            var objectNode = (ObjectNode) node;
            var refNode = objectNode.get("$ref");
            if (refNode != null && refNode.isTextual()) {
                var ref = refNode.asText();
                if (ref.startsWith("#")) {
                    objectNode.put("$ref", docUri + ref);
                } else {
                    var refUri = URI.create(ref);
                    if (!refUri.isAbsolute()) {
                        var documentUri = URI.create(docUri);
                        var restated = documentUri.resolve(refUri);
                        objectNode.put("$ref", restated.toString());
                    }
                }
            }
            for (var property : objectNode.properties()) {
                var shape = SchemaNormalizer.SCHEMA_FIELDS.get(property.getKey());
                if (shape == null) {
                    continue;
                }
                var value = property.getValue();
                if (shape == SchemaNormalizer.SchemaShape.SCHEMA_MAP) {
                    // The keys of a schema map are user-chosen property or definition
                    // names, so its schemas sit one level below the keyword.
                    for (var entry : value.properties()) {
                        qualifyRefs(entry.getValue(), docUri);
                    }
                } else {
                    qualifyRefs(value, docUri);
                }
            }
        } else if (node.isArray()) {
            for (var element : node) {
                qualifyRefs(element, docUri);
            }
        }
    }
}
