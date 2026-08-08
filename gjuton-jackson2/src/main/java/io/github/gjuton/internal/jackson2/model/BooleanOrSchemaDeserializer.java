package io.github.gjuton.internal.jackson2.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.github.gjuton.internal.model.Schema;
import java.io.IOException;

/**
 * Deserializes a JSON Schema keyword that can be either a boolean or a
 * schema object. Returns either boolean objects or schema objects.
 */
class BooleanOrSchemaDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return p.getCodec().readValue(p, Schema.class);
    }
}
