package io.github.gjuton.errors;

/**
 * Thrown when a generated value cannot be bound to the target type requested
 * from {@code Gjuton.generate(Class)}. This indicates the generated JSON does
 * not map onto the class — for example a type mismatch, or a required
 * constructor argument the generated shape does not provide.
 */
public class JsonBindingException extends RuntimeException {

    public JsonBindingException(String message, Throwable cause) {
        super(message, cause);
    }
}
