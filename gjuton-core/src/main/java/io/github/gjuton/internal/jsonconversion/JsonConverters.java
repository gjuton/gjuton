package io.github.gjuton.internal.jsonconversion;

import io.github.gjuton.internal.extension.GjutonExtensions;

/**
 * The {@link JsonConverter} the running classpath provides.
 *
 * <p>JSON conversion comes from an extension, so having one on the
 * classpath is all the configuration there is.
 */
public final class JsonConverters {

    private JsonConverters() {
    }

    /**
     * The converter for this classpath. The same instance every call.
     *
     * @throws IllegalStateException if no extension provides one; the
     *     message names the artifacts that do
     */
    public static JsonConverter get() {
        var locator = GjutonExtensions.locator();
        return locator.find(JsonConverter.class)
                .orElseThrow(() -> new IllegalStateException("No gjuton extension on the classpath provides JSON conversion. Add the artifact matching "
                        + "the Jackson version in use: io.github.gjuton:gjuton-jackson2 for Jackson 2, io.github.gjuton:gjuton-jackson3 for Jackson 3."));
    }
}
