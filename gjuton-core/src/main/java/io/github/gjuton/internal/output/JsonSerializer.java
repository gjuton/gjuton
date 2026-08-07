package io.github.gjuton.internal.output;

import io.github.gjuton.errors.JsonBindingException;
import io.github.gjuton.internal.jsonconversion.JsonConverters;

/**
 * Serializes the generator's in-memory value tree.
 */
public final class JsonSerializer {

    /**
     * Converts a value tree (maps, lists, scalars, nulls) to a compact JSON string.
     *
     * @throws JsonBindingException if the tree cannot be written
     */
    public static String serialize(Object value) {
        var converter = JsonConverters.get();
        return converter.write(value);
    }

    /**
     * Binds a value tree (maps, lists, scalars, nulls) to an instance of
     * {@code type}, as if the tree's JSON form had been read into it.
     *
     * @throws JsonBindingException if the tree does not map onto {@code type}
     */
    public static <T> T convert(Object value, Class<T> type) {
        var converter = JsonConverters.get();
        return converter.convert(value, type);
    }
}
