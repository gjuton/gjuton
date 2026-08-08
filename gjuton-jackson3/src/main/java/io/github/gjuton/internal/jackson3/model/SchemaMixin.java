package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.Schema;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * Supplies {@link Schema}'s deserializer bindings. Naming a deserializer
 * means naming a Jackson databind class, which the model itself does not,
 * so the binding is attached from here instead.
 */
public abstract class SchemaMixin {

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private List<Schema> allOf;

    @JsonDeserialize(using = SchemaDeserializer.class)
    private Schema ifSchema;

    @JsonDeserialize(using = SchemaDeserializer.class)
    private Schema thenSchema;

    @JsonDeserialize(using = SchemaDeserializer.class)
    private Schema elseSchema;

    @JsonDeserialize(using = SchemaDeserializer.class)
    private Schema notSchema;

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private void setOneOf(List<Schema> oneOf) {
    }

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private void setAnyOf(List<Schema> anyOf) {
    }
}
