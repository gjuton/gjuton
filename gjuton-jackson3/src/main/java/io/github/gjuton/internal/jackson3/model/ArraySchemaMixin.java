package io.github.gjuton.internal.jackson3.model;

import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.Schema;
import java.util.List;
import tools.jackson.databind.annotation.JsonDeserialize;

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
