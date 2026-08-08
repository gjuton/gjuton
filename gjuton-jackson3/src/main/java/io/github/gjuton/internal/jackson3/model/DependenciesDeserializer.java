package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.Schema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.node.ObjectNode;

/**
 * Deserializes the Draft 7 {@code dependencies} keyword, whose entries
 * are either an array of property names (keys-form) or a sub-schema
 * (schema-form). Returns a {@code Map<String, Object>} where each value
 * is a {@code List<String>} or a {@link Schema}.
 *
 * <p>Keys-form example: {@code "billing_address": ["street", "city"]}
 * produces a {@code List<String>}.
 *
 * <p>Schema-form example:
 * {@code "billing_address": {"properties": {"street": {"type": "string"}}}}
 * produces a {@link Schema}.
 */
class DependenciesDeserializer extends ValueDeserializer<Map<String, Object>> {

    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) {
        var result = new LinkedHashMap<String, Object>();
        if (p.currentToken() != JsonToken.START_OBJECT) {
            return result;
        }
        while (p.nextToken() != JsonToken.END_OBJECT) {
            var key = p.currentName();
            p.nextToken();
            if (p.currentToken() == JsonToken.START_ARRAY) {
                var list = new ArrayList<String>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    list.add(p.getString());
                }
                result.put(key, List.copyOf(list));
            } else if (p.currentToken() == JsonToken.START_OBJECT) {
                var tree = (ObjectNode) p.readValueAsTree();
                if (!tree.has("type")) {
                    tree.put("type", "object");
                }
                result.put(key, ctxt.readTreeAsValue(tree, Schema.class));
            }
        }
        return result;
    }
}
