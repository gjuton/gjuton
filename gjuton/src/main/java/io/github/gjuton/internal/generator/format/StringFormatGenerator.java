package io.github.gjuton.internal.generator.format;

import static io.github.gjuton.internal.generator.GenerationResult.result;
import static io.github.gjuton.internal.generator.GenerationResult.skip;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.generator.GenerationResult;
import io.github.gjuton.internal.generator.GeneratorContext;
import io.github.gjuton.internal.generator.PhaseGenerator;
import io.github.gjuton.internal.model.StringSchema;
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
        return acceptable(candidate) ? result(candidate) : skip();
    }

    protected final String randomWithRetry() {
        for (int attempt = 0; attempt < RETRY_BUDGET; attempt++) {
            var candidate = generateCandidate();
            if (acceptable(candidate)) {
                return candidate;
            }
        }
        throw new UnsatisfiableSchemaException(
                "Not able to generate a value satisfying the schema's pattern and length constraints",
                context.currentJsonPointer());
    }

    private boolean acceptable(String candidate) {
        if (schema.getMinLength() != null && candidate.length() < schema.getMinLength()) {
            return false;
        }
        if (schema.getMaxLength() != null && candidate.length() > schema.getMaxLength()) {
            return false;
        }
        return compiledPattern == null || compiledPattern.matcher(candidate).find();
    }

    protected abstract String generateCandidate();
}
