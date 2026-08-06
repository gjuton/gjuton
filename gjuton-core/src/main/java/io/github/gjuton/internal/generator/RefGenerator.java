package io.github.gjuton.internal.generator;

/**
 * Generator for schemas with a {@code $ref} keyword. A {@code $ref}
 * replaces the schema it appears in with the referenced schema, allowing
 * schema reuse and recursive definitions.
 */
final class RefGenerator implements Generator<Object> {

    private final GeneratorContext context;
    private final String ref;

    RefGenerator(GeneratorContext context, String ref) {
        this.context = context;
        this.ref = ref;
    }

    @Override
    public Object generate() {
        var schema = context.resolveRef(ref);
        var target = context.generatorFor(schema);
        return target.generate();
    }
}
