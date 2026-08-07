package io.github.gjuton.internal.jsonconversion;

import io.github.gjuton.errors.JsonBindingException;

/**
 * Reads, converts and writes JSON without naming a Jackson version.
 *
 * <p>A tree is plain {@code Map}, {@code List}, {@code String},
 * {@code Boolean}, {@code null} and numbers — an integral number binds to
 * the narrowest of {@code Integer}, {@code Long} or {@code BigInteger}
 * that holds it, a fractional one to {@code Double}.
 *
 * <p>No implementation type escapes: every failure to read, convert or
 * write surfaces as {@link JsonBindingException}.
 */
public interface JsonConverter {

    /**
     * Reads {@code json} into a tree. Content following the first complete
     * value is ignored.
     *
     * @throws JsonBindingException if {@code json} is not valid JSON
     */
    Object readTree(String json);

    /**
     * Binds {@code tree} to an instance of {@code type}, as if the tree's
     * JSON form had been read into it. A property {@code type} does not
     * declare fails the binding, unless {@code type} itself says to ignore
     * unknown properties.
     *
     * @throws JsonBindingException if {@code tree} does not map onto {@code type}
     */
    <T> T convert(Object tree, Class<T> type);

    /**
     * Writes {@code value} as compact JSON, keeping the property order its
     * maps state.
     *
     * @throws JsonBindingException if {@code value} cannot be written
     */
    String write(Object value);
}
