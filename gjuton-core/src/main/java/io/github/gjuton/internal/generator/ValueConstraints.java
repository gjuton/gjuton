package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.util.FunctionalUtil.coalesce;

import io.github.gjuton.internal.util.RandomUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneOffset;

/**
 * Bounds for a generation run, threaded into the generator tree as part of
 * {@link GeneratorConfig}. It appears in two layers: a mode base, where every
 * field is set, and a caller layer, where null means that bound was left alone.
 * {@link GeneratorContext} exposes both, so a generator can tell a bound the
 * caller chose from one gjuton picked for them.
 *
 * <p>{@link #forRandom()} produces narrow, realistic-looking bounds (dates near
 * the current year, moderate number range), while {@link #forExhaustive()}
 * produces wide bounds that leave the full schema range reachable.
 */
public record ValueConstraints(
        Integer stringMinLength,
        Integer stringMaxLength,
        BigDecimal numberMin,
        BigDecimal numberMax,
        Instant dateMin,
        Instant dateMax,
        String alphabet,
        Integer arrayMinLength,
        Integer arrayMaxLength) {

    private static final Instant EXHAUSTIVE_DATE_MIN = Instant.parse("1900-01-01T00:00:00Z");
    private static final Instant EXHAUSTIVE_DATE_MAX = Instant.parse("2099-12-31T23:59:59Z");

    private static final BigDecimal RANDOM_NUMBER_MIN = BigDecimal.valueOf(-1_000_000);
    private static final BigDecimal RANDOM_NUMBER_MAX = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal EXHAUSTIVE_NUMBER_MIN = BigDecimal.valueOf(-Long.MAX_VALUE);
    private static final BigDecimal EXHAUSTIVE_NUMBER_MAX = BigDecimal.valueOf(Long.MAX_VALUE);

    /**
     * How long strings and arrays may get when the caller sets no maximum, so a
     * schema spelling "unbounded" as a huge {@code maxLength} or {@code maxItems}
     * is not taken literally.
     */
    private static final int RANDOM_STRING_MAX_LENGTH = 1_000;
    private static final int EXHAUSTIVE_STRING_MAX_LENGTH = 100_000;
    private static final int RANDOM_ARRAY_MAX_LENGTH = 100;
    private static final int EXHAUSTIVE_ARRAY_MAX_LENGTH = 1_000;

    /**
     * Defaults for {@link io.github.gjuton.api.GenerationMode#RANDOM}: dates
     * span the previous year through the next year and numbers stay within
     * &plusmn;1&thinsp;000&thinsp;000.
     */
    public static ValueConstraints forRandom() {
        int thisYear = Year.now(ZoneOffset.UTC).getValue();
        var dateMin = Year.of(thisYear - 1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var dateMax = Year.of(thisYear + 1).atMonth(12).atEndOfMonth()
                .atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        return new ValueConstraints(
                0,
                RANDOM_STRING_MAX_LENGTH,
                RANDOM_NUMBER_MIN,
                RANDOM_NUMBER_MAX,
                dateMin,
                dateMax,
                RandomUtil.ENGLISH_ALPHABET,
                0,
                RANDOM_ARRAY_MAX_LENGTH);
    }

    /**
     * Defaults for {@link io.github.gjuton.api.GenerationMode#EXHAUSTIVE}:
     * wide bounds that leave boundary-value phases room to reach schema
     * extremes.
     */
    public static ValueConstraints forExhaustive() {
        return new ValueConstraints(
                0,
                EXHAUSTIVE_STRING_MAX_LENGTH,
                EXHAUSTIVE_NUMBER_MIN,
                EXHAUSTIVE_NUMBER_MAX,
                EXHAUSTIVE_DATE_MIN,
                EXHAUSTIVE_DATE_MAX,
                RandomUtil.ENGLISH_ALPHABET,
                0,
                EXHAUSTIVE_ARRAY_MAX_LENGTH);
    }

    /**
     * A caller layer that sets no bound at all, leaving every mode default in
     * force.
     */
    public static ValueConstraints none() {
        return new ValueConstraints(null, null, null, null, null, null, null, null, null);
    }

    /**
     * These bounds with each of the caller's replacing the one it corresponds to.
     * Every field is set in the result, given a base where every field is set.
     */
    public ValueConstraints overlaidWith(ValueConstraints caller) {
        return new ValueConstraints(
                coalesce(caller.stringMinLength, stringMinLength),
                coalesce(caller.stringMaxLength, stringMaxLength),
                coalesce(caller.numberMin, numberMin),
                coalesce(caller.numberMax, numberMax),
                coalesce(caller.dateMin, dateMin),
                coalesce(caller.dateMax, dateMax),
                coalesce(caller.alphabet, alphabet),
                coalesce(caller.arrayMinLength, arrayMinLength),
                coalesce(caller.arrayMaxLength, arrayMaxLength));
    }
}
