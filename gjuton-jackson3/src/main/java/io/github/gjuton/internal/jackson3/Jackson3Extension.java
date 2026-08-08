package io.github.gjuton.internal.jackson3;

import io.github.gjuton.internal.extension.GjutonExtension;
import io.github.gjuton.internal.extension.ServiceRegistry;
import io.github.gjuton.internal.jackson3.conversion.Jackson3JsonConverter;
import io.github.gjuton.internal.jackson3.model.ArraySchemaMixin;
import io.github.gjuton.internal.jackson3.model.NumericSchemaMixin;
import io.github.gjuton.internal.jackson3.model.ObjectSchemaMixin;
import io.github.gjuton.internal.jackson3.model.SchemaMixin;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.Schema;

/**
 * Converts JSON with Jackson 3.
 *
 * <p>Holds the two things that cannot be stated version-neutrally: the
 * converter itself, and how the schema model binds — a model class names
 * no deserializer of its own, so those bindings are attached from here.
 */
public final class Jackson3Extension implements GjutonExtension {

    @Override
    public String name() {
        return "jackson3";
    }

    @Override
    public void init(ServiceRegistry registry) {
        var mapper = Jackson3JsonConverter.mapperBuilder()
                .addMixIn(Schema.class, SchemaMixin.class)
                .addMixIn(ObjectSchema.class, ObjectSchemaMixin.class)
                .addMixIn(ArraySchema.class, ArraySchemaMixin.class)
                .addMixIn(NumericSchema.class, NumericSchemaMixin.class)
                .build();
        registry.register(JsonConverter.class, new Jackson3JsonConverter(mapper));
    }
}
