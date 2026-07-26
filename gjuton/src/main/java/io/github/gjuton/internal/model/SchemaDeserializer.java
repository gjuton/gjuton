package io.github.gjuton.internal.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import java.io.IOException;

/**
 * Handles JSON Schema conventions that Jackson's annotation-driven
 * deserialization cannot express: bare {@code true}/{@code false} as
 * schema objects, and {@code $ref} as a schema replacement (Draft-07
 * §8.3). A {@code $ref} produces a {@link RefSchema} — all sibling
 * keywords are ignored.
 */
class SchemaDeserializer extends JsonDeserializer<Schema> {

    @Override
    public Schema deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return new UntypedSchema();
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return new UnsatisfiableSchema();
        }
        var node = (JsonNode) p.readValueAsTree();
        return fromTree(node, p.getCodec());
    }

    @Override
    public Schema deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return new UntypedSchema();
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return new UnsatisfiableSchema();
        }
        var node = (JsonNode) p.readValueAsTree();
        if (node.isObject() && node.has("$ref")) {
            var refNode = node.get("$ref");
            if (refNode.isTextual()) {
                return RefSchema.builder().ref(refNode.asText()).build();
            }
        }
        var treeParser = node.traverse(p.getCodec());
        treeParser.nextToken();
        return (Schema) typeDeserializer.deserializeTypedFromObject(treeParser, ctxt);
    }

    /**
     * Converts a JSON tree to a {@link Schema}, producing a
     * {@link RefSchema} when {@code $ref} is present.
     */
    static Schema fromTree(JsonNode node, ObjectCodec codec) throws JsonProcessingException {
        if (node.isObject() && node.has("$ref")) {
            var refNode = node.get("$ref");
            if (refNode.isTextual()) {
                return RefSchema.builder().ref(refNode.asText()).build();
            }
        }
        return codec.treeToValue(node, Schema.class);
    }
}
