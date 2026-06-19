package se.plilja.jsonschemagen.internal.generator;

/**
 * Generator for schemas with a {@code const} keyword. Always emits the
 * exact value declared in the schema, regardless of how many times
 * {@link #generate()} is called.
 */
final class ConstGenerator implements Generator<Object> {

    private final Object value;

    ConstGenerator(Object value) {
        this.value = value;
    }

    @Override
    public Object generate() {
        return value;
    }
}
