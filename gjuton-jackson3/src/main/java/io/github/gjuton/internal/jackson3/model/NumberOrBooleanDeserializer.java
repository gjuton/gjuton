package io.github.gjuton.internal.jackson3.model;

import java.math.BigDecimal;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Deserializes a JSON Schema keyword that can be either a number or a
 * boolean. Returns either {@link BigDecimal} or {@link Boolean} objects.
 */
class NumberOrBooleanDeserializer extends ValueDeserializer<Object> {

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return p.getDecimalValue();
    }
}
