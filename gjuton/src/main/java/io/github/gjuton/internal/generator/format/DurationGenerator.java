package io.github.gjuton.internal.generator.format;

import static io.github.gjuton.internal.generator.GenerationResult.result;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.generator.GenerationResult;
import io.github.gjuton.internal.generator.GeneratorContext;
import io.github.gjuton.internal.model.StringSchema;

/**
 * Generates ISO 8601 duration strings (e.g. {@code P3D}, {@code PT1H30M},
 * {@code P2DT3H30M}) for the {@code duration} format.
 *
 * <p>Only the day/time subset of ISO 8601 is used ({@code nD}, {@code nH},
 * {@code nM}, {@code nS}) so that values are parseable by
 * {@link java.time.Duration#parse}.
 */
public final class DurationGenerator extends StringFormatGenerator<DurationGenerator.DurationPhase> {

    private static final int MIN_DURATION_LENGTH = 3;
    private static final int MAX_COMPONENT_VALUE = 99;

    protected enum DurationPhase {
        ZERO, RANDOM
    }

    public DurationGenerator(GeneratorContext context, StringSchema schema) {
        super(DurationPhase.class, context, schema);
    }

    @Override
    protected DurationPhase minimalPhase() {
        return DurationPhase.RANDOM;
    }

    @Override
    protected GenerationResult<String> generatePhase(DurationPhase phase) {
        if (schema.getMaxLength() != null && schema.getMaxLength() < MIN_DURATION_LENGTH) {
            throw new UnsatisfiableSchemaException(
                    "ISO 8601 durations are at least " + MIN_DURATION_LENGTH
                            + " characters; schema length bounds exclude that");
        }
        return switch (phase) {
            case ZERO -> tryCandidate("P0D");
            case RANDOM -> result(randomWithRetry());
        };
    }

    @Override
    protected String generateCandidate() {
        var sb = new StringBuilder("P");
        var random = context.random();

        // Date component (days only — java.time.Duration doesn't support years/months)
        boolean hasDatePart = false;
        if (random.nextBoolean()) {
            sb.append(random.nextInt(MAX_COMPONENT_VALUE + 1)).append('D');
            hasDatePart = true;
        }

        // Time components
        boolean hasTimePart = false;
        var timeSb = new StringBuilder();
        if (random.nextBoolean()) {
            timeSb.append(random.nextInt(MAX_COMPONENT_VALUE + 1)).append('H');
            hasTimePart = true;
        }
        if (random.nextBoolean()) {
            timeSb.append(random.nextInt(MAX_COMPONENT_VALUE + 1)).append('M');
            hasTimePart = true;
        }
        if (random.nextBoolean()) {
            timeSb.append(random.nextInt(MAX_COMPONENT_VALUE + 1)).append('S');
            hasTimePart = true;
        }

        if (hasTimePart) {
            sb.append('T').append(timeSb);
        }

        // At least one component required
        if (!hasDatePart && !hasTimePart) {
            sb.append("0D");
        }

        return sb.toString();
    }
}
