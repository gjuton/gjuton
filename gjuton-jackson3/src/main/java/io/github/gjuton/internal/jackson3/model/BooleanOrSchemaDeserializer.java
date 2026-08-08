package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.Schema;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes a JSON Schema keyword that can be either a boolean or a
 * schema object. Returns either boolean objects or schema objects.
 */
class BooleanOrSchemaDeserializer extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return ctxt.readValue(p, Schema.class);
    }
}
