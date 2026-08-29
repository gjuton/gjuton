package io.github.gjuton.errors;

/**
 * Thrown when a {@code pattern} or a {@code patternProperties} key cannot be
 * used to generate values. That covers a regular expression no engine can read
 * as well as a well-formed one whose constructs Gjuton's regex generation does
 * not support — from a caller's side both mean the same thing: no value can be
 * produced for that position.
 *
 * <p>Unlike {@link UnsatisfiableSchemaException} this is never worked around by
 * choosing a different branch or leaving a property out: a pattern Gjuton
 * cannot use always fails generation, so it is reported rather than hidden.
 *
 * <p>The exception message names the offending pattern and, when available, the
 * path in the generated document where it applies. The exception the regex
 * engine reported is retained as the cause.
 */
public class UnsupportedPatternException extends RuntimeException {

    /**
     * Appends {@code " (at <schemaPath>)"} to the message, naming the document
     * root as {@code $}, so a message carries a location whenever one was known.
     */
    public UnsupportedPatternException(String message, String schemaPath, Throwable cause) {
        super(schemaPath == null
                ? message
                : message + " (at " + (schemaPath.isEmpty() ? "$" : schemaPath) + ")",
                cause);
    }
}
