package io.github.gjuton.internal.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Supplies {@link NumericSchema}'s deserializer bindings. Naming a
 * deserializer means naming a Jackson databind class, which the model
 * itself does not, so the binding is attached from here instead.
 */
public abstract class NumericSchemaMixin {

    @JsonDeserialize(using = NumberOrBooleanDeserializer.class)
    private Object exclusiveMinimum;

    @JsonDeserialize(using = NumberOrBooleanDeserializer.class)
    private Object exclusiveMaximum;
}
