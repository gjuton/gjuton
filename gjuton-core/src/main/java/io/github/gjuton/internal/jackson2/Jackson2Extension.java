package io.github.gjuton.internal.jackson2;

import io.github.gjuton.internal.extension.GjutonExtension;
import io.github.gjuton.internal.extension.ServiceRegistry;
import io.github.gjuton.internal.jackson2.conversion.Jackson2JsonConverter;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.ArraySchemaMixin;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.NumericSchemaMixin;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.ObjectSchemaMixin;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.SchemaMixin;

/**
 * Converts JSON with Jackson 2.
 *
 * <p>Holds the two things that cannot be stated version-neutrally: the
 * converter itself, and how the schema model binds — a model class names
 * no deserializer of its own, so those bindings are attached from here.
 */
public final class Jackson2Extension implements GjutonExtension {

    @Override
    public String name() {
        return "jackson2";
    }

    @Override
    public void init(ServiceRegistry registry) {
        var mapper = Jackson2JsonConverter.mapperBuilder()
                .addMixIn(Schema.class, SchemaMixin.class)
                .addMixIn(ObjectSchema.class, ObjectSchemaMixin.class)
                .addMixIn(ArraySchema.class, ArraySchemaMixin.class)
                .addMixIn(NumericSchema.class, NumericSchemaMixin.class)
                .build();
        registry.register(JsonConverter.class, new Jackson2JsonConverter(mapper));
    }
}
