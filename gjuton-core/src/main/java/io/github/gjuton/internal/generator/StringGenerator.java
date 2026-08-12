package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.generator.GenerationResult.result;
import static io.github.gjuton.internal.generator.GenerationResult.skip;
import static io.github.gjuton.internal.util.FunctionalUtil.coalesce;

import com.github.curiousoddman.rgxgen.RgxGen;
import com.github.curiousoddman.rgxgen.config.RgxGenOption;
import com.github.curiousoddman.rgxgen.config.RgxGenProperties;
import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.StringSchema;
import io.github.gjuton.internal.util.RandomUtil;

/**
 * Generator for {@code "type": "string"} schemas without a recognised
 * {@code format}. Respects {@code minLength}, {@code maxLength}, and
 * {@code pattern} constraints.
 */
final class StringGenerator extends PhaseGenerator<StringGenerator.GenerationPhase, String> {

    private static final int PATTERN_RETRY_BUDGET = 100;

    /**
     * How far an unbounded quantifier in a {@code pattern} may repeat when the
     * schema sets no {@code maxLength}. Nested ones multiply, so it stays low.
     */
    private static final int UNBOUNDED_REPETITION_LIMIT = 8;

    private final StringSchema schema;
    private final RgxGen rgxGen;

    private boolean reportedDefaultMaximumEffect;

    enum GenerationPhase {
        MIN_LENGTH, MAX_LENGTH, EMPTY, RANDOM
    }

    StringGenerator(GeneratorContext context, StringSchema schema) {
        super(GenerationPhase.class, context);
        this.schema = schema;
        boolean callerBoundedLength = context.callerConstraints().stringMaxLength() != null;
        this.rgxGen = schema.getPattern() != null ? buildRgxGen(schema, effectiveMaxLength(), callerBoundedLength) : null;
    }

    @Override
    protected GenerationPhase minimalPhase() {
        return GenerationPhase.RANDOM;
    }

    /**
     * A generator for the schema's {@code pattern}. An unbounded quantifier
     * repeats up to {@code maxLength}, or a few times if nothing bounds it.
     */
    private static RgxGen buildRgxGen(StringSchema schema, int maxLength, boolean callerBoundedLength) {
        Integer schemaMax = schema.getMaxLength();
        boolean unbounded = !callerBoundedLength && (schemaMax == null || schemaMax == Integer.MAX_VALUE);
        int repetition = unbounded ? UNBOUNDED_REPETITION_LIMIT : maxLength;
        var properties = new RgxGenProperties();
        RgxGenOption.INFINITE_PATTERN_REPETITION.setInProperties(properties, repetition);
        return RgxGen.parse(properties, schema.getPattern());
    }

    @Override
    protected GenerationResult<String> generatePhase(GenerationPhase phase) {
        int minLength = effectiveMinLength();
        int maxLength = effectiveMaxLength();
        if (minLength > maxLength) {
            throw new UnsatisfiableSchemaException(
                    "String length bounds are empty after applying constraints: effective minimum " + minLength
                            + " exceeds effective maximum " + maxLength,
                    context.currentJsonPointer());
        }
        if (context.callerConstraints().stringMaxLength() == null && !reportedDefaultMaximumEffect) {
            int defaultMax = context.constraints().stringMaxLength();
            Integer schemaMax = schema.getMaxLength();
            if (minLength > defaultMax) {
                reportedDefaultMaximumEffect = true;
                log.info("{}: generating strings of {} characters, past gjuton's default maximum of {}, to meet the minimum length",
                        context.currentJsonPointer(), minLength, defaultMax);
            } else if (context.isExhaustive() && schemaMax != null && schemaMax > defaultMax) {
                reportedDefaultMaximumEffect = true;
                log.info("{}: limiting strings to gjuton's default maximum of {} characters, not the schema's maxLength of {};"
                                + " set Constraints.stringLength to choose your own",
                        context.currentJsonPointer(), defaultMax, schemaMax);
            }
        }
        if (rgxGen != null) {
            return switch (phase) {
                case MIN_LENGTH -> hasLowerLengthBound() ? generateFromPatternWithLength(minLength) : skip();
                case MAX_LENGTH -> hasUpperLengthBound() ? generateFromPatternWithLength(maxLength) : skip();
                case EMPTY -> minLength == 0 ? generateFromPatternWithLength(0) : skipEmptyPhase(minLength);
                case RANDOM -> result(generateFromPattern());
            };
        }
        return switch (phase) {
            case MIN_LENGTH -> hasLowerLengthBound()
                    ? result(RandomUtil.randomStringOfLength(alphabet(), minLength, context.random()))
                    : skip();
            case MAX_LENGTH -> hasUpperLengthBound()
                    ? result(RandomUtil.randomStringOfLength(alphabet(), maxLength, context.random()))
                    : skip();
            case EMPTY -> minLength == 0 ? result("") : skipEmptyPhase(minLength);
            case RANDOM -> result(randomString());
        };
    }

    /**
     * Declines the EMPTY phase, naming the effective minimum that rules the
     * empty string out.
     */
    private GenerationResult<String> skipEmptyPhase(int minLength) {
        log.trace("skipping the EMPTY phase: the effective minimum length is {}", minLength);
        return skip();
    }

    private int effectiveMinLength() {
        int schemaMin = coalesce(schema.getMinLength(), 0);
        int minInForce = context.constraints().stringMinLength();
        return Math.max(schemaMin, minInForce);
    }

    /**
     * The longest string this generator produces: the tighter of the schema's
     * {@code maxLength} and the maximum in force.
     */
    private int effectiveMaxLength() {
        int limit = context.constraints().stringMaxLength();
        if (context.callerConstraints().stringMaxLength() == null) {
            // A caller's maximum is honored even below the minimum length; a default
            // gives way, so the schema's minimum is still reachable.
            limit = Math.max(limit, effectiveMinLength());
        }
        Integer schemaMax = schema.getMaxLength();
        return schemaMax != null ? Math.min(schemaMax, limit) : limit;
    }

    private boolean hasLowerLengthBound() {
        return schema.getMinLength() != null;
    }

    private boolean hasUpperLengthBound() {
        return schema.getMaxLength() != null;
    }

    private String alphabet() {
        return context.constraints().alphabet();
    }

    private GenerationResult<String> generateFromPatternWithLength(int targetLength) {
        for (int attempt = 0; attempt < PATTERN_RETRY_BUDGET; attempt++) {
            var candidate = rgxGen.generate(context.random());
            if (candidate.length() == targetLength) {
                return result(candidate);
            }
        }
        log.trace("giving up on length {}: {} attempts at pattern '{}' produced no match of that length",
                targetLength, PATTERN_RETRY_BUDGET, schema.getPattern());
        return skip();
    }

    private String generateFromPattern() {
        int min = effectiveMinLength();
        int max = effectiveMaxLength();
        for (int attempt = 0; attempt < PATTERN_RETRY_BUDGET; attempt++) {
            var candidate = rgxGen.generate(context.random());
            if (candidate.length() >= min && candidate.length() <= max) {
                return candidate;
            }
        }
        throw new UnsatisfiableSchemaException(
                "Not able to generate a string matching pattern '" + schema.getPattern()
                        + "' within length bounds [" + min + ", " + max + "]",
                context.currentJsonPointer());
    }

    private String randomString() {
        int min = effectiveMinLength();
        int max = Math.min(min + 20, effectiveMaxLength());
        int length = min == max ? min : context.random().nextInt(min, max + 1);
        return RandomUtil.randomStringOfLength(alphabet(), length, context.random());
    }
}
