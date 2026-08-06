package io.github.gjuton.internal.generator.format;

import static io.github.gjuton.internal.generator.GenerationResult.result;
import static io.github.gjuton.internal.generator.GenerationResult.skip;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.generator.GenerationResult;
import io.github.gjuton.internal.generator.GeneratorContext;
import io.github.gjuton.internal.generator.PhaseGenerator;
import io.github.gjuton.internal.model.StringSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * Base class for generators of string schemas with a recognised {@code format}.
 */
abstract class StringFormatGenerator<E extends Enum<E>> extends PhaseGenerator<E, String> {

    protected static final int RETRY_BUDGET = 100;

    protected final StringSchema schema;
    private final Pattern compiledPattern;

    protected StringFormatGenerator(Class<E> phaseClass, GeneratorContext context, StringSchema schema) {
        super(phaseClass, context);
        this.schema = schema;
        this.compiledPattern = schema.getPattern() != null ? Pattern.compile(schema.getPattern()) : null;
        if (schema.getMinLength() != null && schema.getMaxLength() != null
                && schema.getMinLength() > schema.getMaxLength()) {
            throw new UnsatisfiableSchemaException(
                "minLength (" + schema.getMinLength() + ") is greater than maxLength (" + schema.getMaxLength() + ")",
                context.currentJsonPointer());
        }
    }

    protected final GenerationResult<String> tryCandidate(String candidate) {
        if (!acceptable(candidate)) {
            log.trace("rejecting the {} candidate '{}': it violates the schema's pattern or length bounds",
                    schema.getFormat(), candidate);
            return skip();
        }
        return result(candidate);
    }

    protected final String randomWithRetry() {
        var rejections = new LinkedHashMap<String, Integer>();
        String lastCandidate = null;
        for (int attempt = 0; attempt < RETRY_BUDGET; attempt++) {
            var candidate = generateCandidate();
            var rejection = rejection(candidate);
            if (rejection == null) {
                return candidate;
            }
            lastCandidate = candidate;
            rejections.merge(rejection, 1, Integer::sum);
        }
        // Logged as one summary rather than per candidate: the budget is large,
        // and which constraint did the rejecting is what identifies the one the
        // candidates are blind to.
        log.trace("no {} candidate accepted in {} attempts against {}: rejected on {}; last candidate '{}'",
                schema.getFormat(), RETRY_BUDGET, constraintSummary(), rejections, lastCandidate);
        throw new UnsatisfiableSchemaException(
                "Not able to generate a value satisfying the schema's " + constraintSummary(),
                context.currentJsonPointer());
    }

    /**
     * Names the constraints a candidate has to clear, as they appear in the
     * schema. Only the constraints actually declared are named, so a schema
     * carrying just one of them is not reported as failing both.
     */
    private String constraintSummary() {
        var parts = new ArrayList<String>();
        if (schema.getPattern() != null) {
            parts.add("pattern " + schema.getPattern());
        }
        if (schema.getMinLength() != null) {
            parts.add("minLength " + schema.getMinLength());
        }
        if (schema.getMaxLength() != null) {
            parts.add("maxLength " + schema.getMaxLength());
        }
        return String.join(", ", parts);
    }

    private boolean acceptable(String candidate) {
        return rejection(candidate) == null;
    }

    /**
     * Names the constraint {@code candidate} fails, or {@code null} if it
     * meets them all. The sole account of what a candidate has to clear, so
     * a constraint added here is counted and reported without further work.
     */
    private String rejection(String candidate) {
        if (!withinLengthBounds(candidate)) {
            return "length";
        }
        if (compiledPattern != null && !compiledPattern.matcher(candidate).find()) {
            return "pattern";
        }
        return null;
    }

    private boolean withinLengthBounds(String candidate) {
        if (schema.getMinLength() != null && candidate.length() < schema.getMinLength()) {
            return false;
        }
        return schema.getMaxLength() == null || candidate.length() <= schema.getMaxLength();
    }

    protected abstract String generateCandidate();
}
