package io.github.gjuton.internal.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Deserializes a JSON Schema keyword that can be either a number or a
 * boolean. Returns either {@link BigDecimal} or {@link Boolean} objects.
 */
class NumberOrBooleanDeserializer extends JsonDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return p.getDecimalValue();
    }
}
