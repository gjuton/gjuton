package io.github.gjuton.errors;

/**
 * Thrown when the generator cannot produce a value that satisfies the schema.
 * This typically indicates either an over-constrained schema (no valid value
 * exists) or one where the generator's random search exhausted its retry
 * budget without finding one.
 *
 * <p>The exception message describes the specific constraint that could not
 * be satisfied and, when available, the path in the generated document where
 * the problem was detected.
 */
public class UnsatisfiableSchemaException extends RuntimeException {

    public UnsatisfiableSchemaException(String message) {
        super(message);
    }

    /**
     * Appends {@code " (at <schemaPath>)"} to the message unless the path
     * is null or empty. Callers without a path should use the single-arg
     * constructor to avoid ambiguity with the {@code (String, Throwable)}
     * overload.
     */
    public UnsatisfiableSchemaException(String message, String schemaPath) {
        super(schemaPath != null && !schemaPath.isEmpty() && !"$".equals(schemaPath)
                ? message + " (at " + schemaPath + ")"
                : message);
    }

    public UnsatisfiableSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
}
