package se.plilja.jsonschemagen.internal.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;

/**
 * Deserializes the JSON Schema {@code additionalProperties} keyword, which
 * can be either a boolean or a schema object. Returns {@link Boolean#TRUE} or
 * {@link Boolean#FALSE} for boolean values, or a {@link Schema} instance for
 * schema objects.
 */
class AdditionalPropertiesDeserializer extends JsonDeserializer<Object> {

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
