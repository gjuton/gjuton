package se.plilja.jsonschemagen.internal.generator.format;

import static se.plilja.jsonschemagen.internal.generator.FunctionalUtil.coalesce;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.skip;
import static se.plilja.jsonschemagen.internal.generator.MathUtil.clampRange;

import java.util.Random;
import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.generator.MathUtil;
import se.plilja.jsonschemagen.internal.generator.RandomUtil;
import se.plilja.jsonschemagen.internal.model.StringSchema;

/**
 * Emits values for the {@code uri-reference} format (RFC 3986 §4.1).
 *
 * <p>A URI reference is either an absolute URI ({@code scheme://...}) or a relative reference
 * (e.g. {@code a/b?q=1#frag}, or the empty string).
 */
public final class UriReferenceGenerator extends StringFormatGenerator<UriReferenceGenerator.UriReferencePhase> {

    static final int MAX_LENGTH = 4096;
    static final int MIN_ABSOLUTE_URI = "http://".length() + HostnameGenerator.minReachable(Alphabets.EN);
    static final int MIN_ABSOLUTE_HTTPS_URI = "https://".length() + HostnameGenerator.minReachable(Alphabets.EN);

    private static final int MAX_SEGMENT_LEN = 8;
    private static final int DEFAULT_LONG_TARGET = 80;
    private static final int HOSTNAME_TYPICAL_MAX = 30;

    public enum UriReferencePhase {
        EMPTY, RELATIVE, ABSOLUTE, RANDOM
    }

    public UriReferenceGenerator(GeneratorContext context, StringSchema schema) {
        super(UriReferencePhase.class, context, schema);
        if (schema.getMinLength() != null && schema.getMinLength() > MAX_LENGTH) {
            throw new UnsatisfiableSchemaException(
                    "URI references produced by this generator cap at " + MAX_LENGTH
                            + " characters; schema length bounds exclude that");
        }
    }

    @Override
    protected UriReferencePhase minimalPhase() {
        return UriReferencePhase.EMPTY;
    }

    @Override
    protected GenerationResult<String> generatePhase(UriReferencePhase phase) {
        return switch (phase) {
            case EMPTY -> tryCandidate("");
            case RELATIVE -> tryCandidate(randomRelativePath(schema, context.random()));
            case ABSOLUTE -> randomAbsoluteUriOfLength();
            case RANDOM -> result(randomWithRetry());
        };
    }

    @Override
    protected String generateCandidate() {
        var random = context.random();
        boolean canAbsolute = coalesce(schema.getMaxLength(), DEFAULT_LONG_TARGET) >= MIN_ABSOLUTE_URI;
        if (canAbsolute && random.nextBoolean()) {
            return randomAbsoluteUri(schema, random);
        }
        return randomRelativePath(schema, random);
    }

    /**
     * Emits an absolute URI candidate for the ABSOLUTE phase, sized to the schema's maxLength
     * when set. Skips when maxLength is smaller than the shortest absolute URI we can produce.
     */
    private GenerationResult<String> randomAbsoluteUriOfLength() {
        if (coalesce(schema.getMaxLength(), DEFAULT_LONG_TARGET) < MIN_ABSOLUTE_URI) {
            return skip();
        }
        return tryCandidate(randomAbsoluteUriOfLength("http", uriLengthBounds(schema).max(), context.random()));
    }

    /**
     * Builds an absolute URI of exactly {@code target} characters: {@code scheme://host[/path]}.
     *
     * @throws IllegalArgumentException if {@code target} is shorter than the shortest reachable hostname
     */
    static String randomAbsoluteUriOfLength(String scheme, int target, Random random) {
        var prefix = scheme + "://";
        int budget = target - prefix.length();
        int minHostname = HostnameGenerator.minReachable(Alphabets.EN);
        if (budget < minHostname) {
            throw new IllegalArgumentException(
                    "target " + target + " too small for scheme '" + scheme
                            + "' (minimum " + (prefix.length() + minHostname) + ")");
        }
        int hostLen = pickHostLength(budget, minHostname, random);
        var host = HostnameGenerator.randomHostname(Alphabets.EN, random, hostLen);
        if (hostLen == budget) {
            return prefix + host;
        }
        int pathLen = budget - hostLen - 1;
        return prefix + host + "/" + randomRelativePath(pathLen, random);
    }

    static String randomShortUri(StringSchema schema, Random random) {
        return UriReferenceGenerator.randomAbsoluteUriOfLength("http", uriLengthBounds(schema).min(), random);
    }

    static String randomLongUri(StringSchema schema, Random random) {
        int length = uriLengthBounds(schema).max();
        var scheme = length >= MIN_ABSOLUTE_HTTPS_URI ? "https" : "http";
        return UriReferenceGenerator.randomAbsoluteUriOfLength(scheme, length, random);
    }

    /**
     * Builds an absolute URI of a length randomly chosen between {@code schema.minLength} and
     * {@code schema.maxLength} (defaults: {@link #MIN_ABSOLUTE_URI} and {@link #DEFAULT_LONG_TARGET}).
     */
    static String randomAbsoluteUri(StringSchema schema, Random random) {
        int length = uriLengthBounds(schema).pickRandom(random);
        var scheme = length >= MIN_ABSOLUTE_HTTPS_URI && random.nextBoolean() ? "https" : "http";
        return randomAbsoluteUriOfLength(scheme, length, random);
    }

    /**
     * Computes the effective length range for an absolute URI generated from {@code schema},
     * clamped to {@code [MIN_ABSOLUTE_URI, MAX_LENGTH]}.
     */
    private static MathUtil.IntRange uriLengthBounds(StringSchema schema) {
        return clampRange(
                coalesce(schema.getMinLength(), MIN_ABSOLUTE_URI),
                coalesce(schema.getMaxLength(), DEFAULT_LONG_TARGET),
                MIN_ABSOLUTE_URI,
                MAX_LENGTH);
    }

    private static int pickHostLength(int budget, int minHostname, Random random) {
        if (budget <= minHostname + 1) {
            return minHostname;
        }
        boolean bareAuthority = budget <= HOSTNAME_TYPICAL_MAX && random.nextInt(4) == 0;
        if (bareAuthority) {
            return budget;
        }
        int maxHost = Math.min(HOSTNAME_TYPICAL_MAX, budget - 2);
        return random.nextInt(minHostname, maxHost + 1);
    }

    static String randomRelativePath(StringSchema schema, Random random) {
        var range = clampRange(
                coalesce(schema.getMinLength(), 0),
                coalesce(schema.getMaxLength(), MAX_LENGTH),
                0,
                MAX_LENGTH);
        return randomRelativePath(range.pickRandom(random), random);
    }

    /**
     * Builds a relative path of exactly {@code length} characters.
     *
     * <p>For example {@code foo/bar/baz}.
     */
    static String randomRelativePath(int length, Random random) {
        if (length == 0) {
            return "";
        }
        var sb = new StringBuilder(length);
        while (sb.length() < length) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            int remaining = length - sb.length();
            int segLen = remaining <= MAX_SEGMENT_LEN
                    ? remaining
                    : random.nextInt(1, Math.min(MAX_SEGMENT_LEN, remaining - 2) + 1);
            sb.append(RandomUtil.randomStringOfLength(RandomUtil.ENGLISH_ALPHABET, segLen, random));
        }
        return sb.toString();
    }
}
