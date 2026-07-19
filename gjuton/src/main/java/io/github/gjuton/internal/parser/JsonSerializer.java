package io.github.gjuton.internal.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gjuton.errors.JsonBindingException;

/**
 * Serializes the generator's in-memory value tree.
 */
public final class JsonSerializer {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Converts a value tree (maps, lists, scalars, nulls) to a compact JSON string.
     */
    public static String serialize(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize generated value", e);
        }
    }

    /**
     * Binds a value tree (maps, lists, scalars, nulls) to an instance of
     * {@code type}, as if the tree's JSON form had been read into it.
     *
     * @throws JsonBindingException if the tree does not map onto {@code type}
     */
    public static <T> T convert(Object value, Class<T> type) {
        try {
            return MAPPER.convertValue(value, type);
        } catch (IllegalArgumentException e) {
            throw new JsonBindingException("Generated value does not map onto " + type.getName(), e);
        }
    }
}
