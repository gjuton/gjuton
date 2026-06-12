package se.plilja.jsonschemagen.internal.generator.format;

import static se.plilja.jsonschemagen.internal.generator.FunctionalUtil.coalesce;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import se.plilja.jsonschemagen.internal.generator.GenerationResult;
import se.plilja.jsonschemagen.internal.generator.GeneratorContext;
import se.plilja.jsonschemagen.internal.generator.StringUtil;
import se.plilja.jsonschemagen.internal.model.StringSchema;

public final class EmailGenerator extends StringFormatGenerator<EmailGenerator.EmailPhase> {

    private static final String[] TLDS = {
            "com", "org", "net", "io", "co", "ai", "dev", "app",
            "info", "biz", "uk", "us", "de", "jp", "fr", "se",
    };

    enum EmailPhase {
        SHORT, LONG, RANDOM
    }

    public EmailGenerator(GeneratorContext context, StringSchema schema) {
        super(EmailPhase.class, context, schema);
    }

    @Override
    protected EmailPhase minimalPhase() {
        // SHORT/LONG can skip when tight bounds reject the canonical; RANDOM is the only
        // non-skippable phase here, as required by the PhaseGenerator contract.
        return EmailPhase.RANDOM;
    }

    @Override
    protected GenerationResult<String> generatePhase(EmailPhase phase) {
        return switch (phase) {
            case SHORT -> tryCandidate(shortEmail());
            case LONG -> tryCandidate(longEmail());
            case RANDOM -> result(randomWithRetry());
        };
    }

    private String shortEmail() {
        int target = Math.max(6, coalesce(schema.getMinLength(), 0));
        int localLen = Math.max(1, target - "@b.co".length());
        return "a".repeat(localLen) + "@b.co";
    }

    private String longEmail() {
        // 64 cap matches RFC 5321 local-part max so the value stays validator-strict.
        int target = coalesce(schema.getMaxLength(), 30);
        int localLen = Math.max(1, Math.min(64, target - "@example.com".length()));
        return "a".repeat(localLen) + "@example.com";
    }

    @Override
    protected String generateCandidate() {
        int localLen = context.random().nextInt(1, 8);
        int domainLen = context.random().nextInt(1, 8);
        var tld = TLDS[context.random().nextInt(TLDS.length)];
        return StringUtil.randomStringOfLength(localLen, context.random())
                + "@" + StringUtil.randomStringOfLength(domainLen, context.random())
                + "." + tld;
    }
}
