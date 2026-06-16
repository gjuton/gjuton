package se.plilja.jsonschemagen.internal.generator.format;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import java.util.Random;
import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.model.StringSchema;

/**
 * Emits values for the {@code uri} format (RFC 3986). A URI is the absolute form of a URI
 * reference; this generator delegates URI construction to
 * {@link UriReferenceGenerator#randomAbsoluteUriOfLength(String, int, Random)} and restricts output to the
 * absolute branch (plus a small mix of {@code mailto:} and {@code telnet://} schemes from
 * {@link #generateCandidate()}).
 */
public final class UriGenerator extends StringFormatGenerator<UriGenerator.UriPhase> {

    public enum UriPhase {
        SHORT, LONG, RANDOM
    }

    public UriGenerator(GeneratorContext context, StringSchema schema) {
        super(UriPhase.class, context, schema);
        if (schema.getMinLength() != null && schema.getMinLength() > UriReferenceGenerator.MAX_LENGTH) {
            throw new UnsatisfiableSchemaException(
                    "URIs produced by this generator cap at " + UriReferenceGenerator.MAX_LENGTH
                            + " characters; schema length bounds exclude that");
        }
        if (schema.getMaxLength() != null && schema.getMaxLength() < UriReferenceGenerator.MIN_ABSOLUTE_URI) {
            throw new UnsatisfiableSchemaException(
                    "URIs produced by this generator are at least " + UriReferenceGenerator.MIN_ABSOLUTE_URI
                            + " characters; schema maxLength excludes that");
        }
    }

    @Override
    protected UriPhase minimalPhase() {
        return UriPhase.SHORT;
    }

    @Override
    protected GenerationResult<String> generatePhase(UriPhase phase) {
        return switch (phase) {
            case SHORT -> tryCandidate(UriReferenceGenerator.randomShortUri(schema, context.random()));
            case LONG -> tryCandidate(UriReferenceGenerator.randomLongUri(schema, context.random()));
            case RANDOM -> result(randomWithRetry());
        };
    }

    @Override
    protected String generateCandidate() {
        var random = context.random();
        return switch (random.nextInt(10)) {
            case 0 -> "telnet://" + Ipv4Generator.randomIpv4(random) + "/";
            case 1 -> "mailto:" + EmailGenerator.randomEmail(Alphabets.EN, random);
            default -> UriReferenceGenerator.randomAbsoluteUri(schema, random);
        };
    }
}
