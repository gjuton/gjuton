package io.github.gjuton.internal.generator;

import java.util.function.Supplier;

/**
 * Stands in for a position the caller registered an override against,
 * yielding their value in place of a generated one. It applies even where
 * the schema is unsatisfiable or endlessly recursive, and is exempt from
 * validation.
 */
final class OverrideGenerator implements Generator<Object> {

    private final Supplier<Object> override;

    OverrideGenerator(Supplier<Object> override) {
        this.override = override;
    }

    @Override
    public Object generate() {
        return new OverriddenValue(override.get());
    }
}
