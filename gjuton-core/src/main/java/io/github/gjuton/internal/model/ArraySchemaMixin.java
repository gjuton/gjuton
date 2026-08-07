package io.github.gjuton.internal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

/**
 * Supplies {@link ArraySchema}'s deserializer bindings. Naming a
 * deserializer means naming a Jackson databind class, which the model
 * itself does not, so the binding is attached from here instead.
 */
public abstract class ArraySchemaMixin {

    @JsonDeserialize(using = ItemsDeserializer.class)
    private Object items;

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private List<Schema> prefixItems;

    @JsonDeserialize(using = BooleanOrSchemaDeserializer.class)
    private Object additionalItems;

    @JsonDeserialize(using = SchemaDeserializer.class)
    private void setContains(Schema contains) {
    }
}
