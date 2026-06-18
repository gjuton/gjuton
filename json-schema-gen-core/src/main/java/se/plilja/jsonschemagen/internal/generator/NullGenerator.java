package se.plilja.jsonschemagen.internal.generator;

final class NullGenerator implements Generator<Object> {

    @Override
    public Object generate() {
        return null;
    }
}
