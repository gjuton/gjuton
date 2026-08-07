package io.github.gjuton.internal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Map;

/**
 * Supplies {@link ObjectSchema}'s deserializer bindings. Naming a
 * deserializer means naming a Jackson databind class, which the model
 * itself does not, so the binding is attached from here instead.
 */
public abstract class ObjectSchemaMixin {

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private Map<String, Schema> properties;

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private Map<String, Schema> patternProperties;

    @JsonDeserialize(using = BooleanOrSchemaDeserializer.class)
    private Object additionalProperties;

    @JsonDeserialize(using = DependenciesDeserializer.class)
    private Map<String, Object> dependencies;

    @JsonDeserialize(contentUsing = SchemaDeserializer.class)
    private Map<String, Schema> dependentSchemas;
}
