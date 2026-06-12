package se.plilja.jsonschemagen.internal.generator.format;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.generator.StringUtil;
import se.plilja.jsonschemagen.internal.model.StringSchema;

public final class HostnameGenerator extends StringFormatGenerator<HostnameGenerator.HostnamePhase> {

    private static final int MIN_LABELS = 2;
    private static final int MAX_LABELS = 4;
    private static final int MIN_LABEL_LEN = 1;
    private static final int MAX_LABEL_LEN = 8;
    // Shortest reachable output is "a.b" (3); longest is MAX_LABELS labels of MAX_LABEL_LEN
    // joined by dots: 4 * 8 + 3 = 35.
    private static final int MIN_REACHABLE_LENGTH = MIN_LABELS + (MIN_LABELS - 1);
    private static final int MAX_REACHABLE_LENGTH = MAX_LABELS * MAX_LABEL_LEN + (MAX_LABELS - 1);

    protected enum HostnamePhase {
        RANDOM
    }

    public HostnameGenerator(GeneratorContext context, StringSchema schema) {
        super(HostnamePhase.class, context, schema);
    }

    @Override
    protected HostnamePhase minimalPhase() {
        return HostnamePhase.RANDOM;
    }

    @Override
    protected GenerationResult<String> generatePhase(HostnamePhase phase) {
        if (schema.getMinLength() != null && schema.getMinLength() > MAX_REACHABLE_LENGTH
                || schema.getMaxLength() != null && schema.getMaxLength() < MIN_REACHABLE_LENGTH) {
            throw new UnsatisfiableSchemaException(
                    "Hostnames produced by this generator are between " + MIN_REACHABLE_LENGTH
                            + " and " + MAX_REACHABLE_LENGTH + " characters; schema length bounds exclude that");
        }
        return result(randomWithRetry());
    }

    @Override
    protected String generateCandidate() {
        int labelCount = context.random().nextInt(MIN_LABELS, MAX_LABELS + 1);
        var sb = new StringBuilder();
        for (int i = 0; i < labelCount; i++) {
            if (i > 0) {
                sb.append('.');
            }
            int len = context.random().nextInt(MIN_LABEL_LEN, MAX_LABEL_LEN + 1);
            sb.append(StringUtil.randomStringOfLength(len, context.random()));
        }
        return sb.toString();
    }
}
