package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes the JSON Schema {@code items} keyword.
 *
 * <p>The keyword accepts several shapes:
 * <ul>
 *     <li>A schema object — returns a {@link Schema} (uniform items)</li>
 *     <li>An array of schemas — returns a {@code List<Schema>} (Draft 7 tuple)</li>
 *     <li>{@code true} — returns {@link UntypedSchema} (any element allowed)</li>
 *     <li>{@code false} — returns {@link UnsatisfiableSchema} (no elements allowed)</li>
 * </ul>
 */
class ItemsDeserializer extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return new UntypedSchema();
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return new UnsatisfiableSchema();
        }
        if (p.currentToken() == JsonToken.START_ARRAY) {
            var schemas = new ArrayList<Schema>();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                schemas.add(ctxt.readValue(p, Schema.class));
            }
            return List.copyOf(schemas);
        }
        return ctxt.readValue(p, Schema.class);
    }
}
