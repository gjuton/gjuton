package io.github.gjuton.internal.parser;

import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.Schema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
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

    /**
     * What {@link #resolvePointer} answers when a document holds nothing at
     * the position a pointer names. Distinct from a JSON {@code null}, which
     * is a value the document does hold.
     */
    private static final Object MISSING = new Object();

    private final JsonConverter converter;

    private RefCollector(JsonConverter converter) {
        this.converter = converter;
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
    static Map<String, Schema> collect(Object rootNode, Schema rootSchema, URI retrievalUri, JsonConverter converter) {
        var collector = new RefCollector(converter);
        var refs = new HashMap<String, Schema>();
        // Self-reference always resolves to the same root Schema instance so phase state
        // is shared between the root and any "#" ref.
        refs.put("#", rootSchema);
        // The root's own $id is applied by the walk, so the retrieval URI is what
        // encloses it. Naming that identity is a second way to refer to the document
        // itself, besides "#".
        var entryIdentity = baseUriOf(rootNode, retrievalUri);
        var entryDocUri = entryIdentity != null ? entryIdentity.toString() : null;
        collector.walk(rootNode, rootNode, entryDocUri, retrievalUri, refs, new HashMap<>());
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
    private void walk(Object node, Object currentDoc, String currentDocUri, URI enclosingBase, Map<String, Schema> refs,
            Map<URI, Object> loadedDocuments) {
        if (node instanceof Map<?, ?> objectNode) {
            var baseUri = baseUriOf(node, enclosingBase);
            var refNode = objectNode.get("$ref");
            if (refNode instanceof String ref) {
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
                    var target = resolvePointer(targetDoc, fragment);
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
            for (var property : objectNode.entrySet()) {
                var shape = SchemaNormalizer.SCHEMA_FIELDS.get(property.getKey());
                if (shape == null) {
                    continue;
                }
                var value = property.getValue();
                if (shape == SchemaNormalizer.SchemaShape.SCHEMA_MAP) {
                    // The keys of a schema map are user-chosen property or definition
                    // names, so its schemas sit one level below the keyword.
                    if (value instanceof Map<?, ?> mapValue) {
                        for (var entry : mapValue.entrySet()) {
                            walk(entry.getValue(), currentDoc, currentDocUri, baseUri, refs, loadedDocuments);
                        }
                    }
                } else {
                    walk(value, currentDoc, currentDocUri, baseUri, refs, loadedDocuments);
                }
            }
        } else if (node instanceof List<?> arrayNode) {
            // Only reachable from a whitelisted keyword above, so an array of
            // schemas is walked while an enum payload is never entered.
            for (var element : arrayNode) {
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
    private Schema resolveFragment(String pointer, Object document) {
        if (pointer.isEmpty()) {
            return converter.convert(document, Schema.class);
        }
        var target = resolvePointer(document, pointer);
        if (target == MISSING) {
            throw new IllegalArgumentException("Unresolved $ref fragment: #" + pointer);
        }
        var schema = converter.convert(target, Schema.class);
        if (schema == null) {
            throw new IllegalArgumentException("$ref target is not a schema: #" + pointer);
        }
        return schema;
    }

    /**
     * The value {@code pointer} names within {@code document}, or
     * {@link #MISSING} when the document holds nothing there. An empty
     * pointer names {@code document} itself.
     *
     * <p>The answer is the live sub-tree, not a copy: changing it changes
     * {@code document}.
     *
     * <p>Escapes are read as RFC 6901 writes them — {@code ~1} for a
     * {@code /} within a key and {@code ~0} for a {@code ~}. Percent-encoding
     * is left as written (issue #150).
     */
    private static Object resolvePointer(Object document, String pointer) {
        if (pointer.isEmpty()) {
            return document;
        }
        if (!pointer.startsWith("/")) {
            return MISSING;
        }
        var current = document;
        // The leading "/" opens the first segment rather than separating two, so the
        // split starts past it. A trailing "/" names the empty key, hence the -1 limit.
        var segments = pointer.substring(1).split("/", -1);
        for (var segment : segments) {
            // A pointer separates segments with /, so a key holding / or ~ arrives
            // escaped: ~1 for /, ~0 for ~. Unescape ~1 first — taking ~0 first turns
            // "~01" into "~1", which the next replace reads as an escape, collapsing a
            // key literally named "~1" to "/".
            var slashesRestored = segment.replace("~1", "/");
            var key = slashesRestored.replace("~0", "~");
            if (current instanceof Map<?, ?> objectNode) {
                if (!objectNode.containsKey(key)) {
                    return MISSING;
                }
                current = objectNode.get(key);
            } else if (current instanceof List<?> arrayNode) {
                int index = asIndex(key);
                if (index < 0 || index >= arrayNode.size()) {
                    return MISSING;
                }
                current = arrayNode.get(index);
            } else {
                return MISSING;
            }
        }
        return current;
    }

    /**
     * The array position {@code segment} names, or {@code -1} when it names
     * no position at all. Only the canonical spelling counts: a leading zero
     * or a sign names nothing.
     */
    private static int asIndex(String segment) {
        if (segment.isEmpty() || segment.length() > 10 || (segment.length() > 1 && segment.charAt(0) == '0')) {
            return -1;
        }
        for (int i = 0; i < segment.length(); i++) {
            char digit = segment.charAt(i);
            if (digit < '0' || digit > '9') {
                return -1;
            }
        }
        return Integer.parseInt(segment);
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
    private static URI baseUriOf(Object node, URI enclosingBase) {
        if (!(node instanceof Map<?, ?> objectNode)) {
            return enclosingBase;
        }
        var idNode = objectNode.containsKey("$id") ? objectNode.get("$id") : objectNode.get("id");
        if (!(idNode instanceof String declaredIdText)) {
            return enclosingBase;
        }
        var declaredId = URI.create(declaredIdText);
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
    private Object loadExternalDocument(URI location, Map<URI, Object> loadedDocuments) {
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
            var document = converter.readTree(content);
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
    private static void qualifyRefs(Object node, String docUri) {
        if (node instanceof Map) {
            @SuppressWarnings("unchecked")
            var objectNode = (Map<String, Object>) node;
            var refNode = objectNode.get("$ref");
            if (refNode instanceof String ref) {
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
            for (var property : objectNode.entrySet()) {
                var shape = SchemaNormalizer.SCHEMA_FIELDS.get(property.getKey());
                if (shape == null) {
                    continue;
                }
                var value = property.getValue();
                if (shape == SchemaNormalizer.SchemaShape.SCHEMA_MAP) {
                    // The keys of a schema map are user-chosen property or definition
                    // names, so its schemas sit one level below the keyword.
                    if (value instanceof Map<?, ?> mapValue) {
                        for (var entry : mapValue.entrySet()) {
                            qualifyRefs(entry.getValue(), docUri);
                        }
                    }
                } else {
                    qualifyRefs(value, docUri);
                }
            }
        } else if (node instanceof List<?> arrayNode) {
            for (var element : arrayNode) {
                qualifyRefs(element, docUri);
            }
        }
    }
}
