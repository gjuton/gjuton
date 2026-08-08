package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;

/**
 * Handles the JSON Schema convention where a bare {@code true} or
 * {@code false} can stand in for a schema object. Returns
 * {@link UntypedSchema} for {@code true}, {@link UnsatisfiableSchema}
 * for {@code false}, and delegates to the default deserializer otherwise.
 */
class SchemaDeserializer extends ValueDeserializer<Schema> {

    @Override
    public Schema deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return new UntypedSchema();
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return new UnsatisfiableSchema();
        }
        return ctxt.readValue(p, Schema.class);
    }

    @Override
    public Schema deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) {
        if (p.currentToken() == JsonToken.VALUE_TRUE) {
            return new UntypedSchema();
        }
        if (p.currentToken() == JsonToken.VALUE_FALSE) {
            return new UnsatisfiableSchema();
        }
        return (Schema) typeDeserializer.deserializeTypedFromObject(p, ctxt);
    }
}
