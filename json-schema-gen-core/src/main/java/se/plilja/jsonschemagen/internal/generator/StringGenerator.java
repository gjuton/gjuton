package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.FunctionalUtil.coalesce;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;
import static se.plilja.jsonschemagen.internal.generator.GenerationResult.skip;

import com.github.curiousoddman.rgxgen.RgxGen;
import java.util.Random;
import se.plilja.jsonschemagen.internal.model.StringSchema;

final class StringGenerator extends PhaseGenerator<StringGenerator.GenerationPhase, String> {

    // TODO consider generating more tricky characters such as newlines <, > and so on
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    private final Random random;
    private final StringSchema schema;
    private final RgxGen rgxGen;

    enum GenerationPhase {
        MIN_LENGTH, MAX_LENGTH, EMPTY, RANDOM
    }

    StringGenerator(Random random, StringSchema schema) {
        super(GenerationPhase.class);
        this.random = random;
        this.schema = schema;
        this.rgxGen = schema.getPattern() != null ? RgxGen.parse(schema.getPattern()) : null;
    }

    @Override
    protected GenerationResult<String> generatePhase(GenerationPhase phase) {
        if (rgxGen != null) {
            return switch (phase) {
                case MIN_LENGTH -> schema.getMinLength() != null ? generateFromPatternWithLength(schema.getMinLength()) : skip();
                case MAX_LENGTH -> schema.getMaxLength() != null ? generateFromPatternWithLength(schema.getMaxLength()) : skip();
                case EMPTY -> {
                    int min = coalesce(schema.getMinLength(), 0);
                    yield min == 0 ? generateFromPatternWithLength(0) : skip();
                }
                case RANDOM -> result(generateFromPattern());
            };
        }
        return switch (phase) {
            case MIN_LENGTH -> schema.getMinLength() != null ? result(randomStringOfLength(schema.getMinLength())) : skip();
            case MAX_LENGTH -> schema.getMaxLength() != null ? result(randomStringOfLength(schema.getMaxLength())) : skip();
            case EMPTY -> {
                int min = coalesce(schema.getMinLength(), 0);
                yield min == 0 ? result("") : skip();
            }
            case RANDOM -> result(randomString());
        };
    }

    private GenerationResult<String> generateFromPatternWithLength(int targetLength) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = rgxGen.generate(random);
            if (candidate.length() == targetLength) {
                return result(candidate);
            }
        }
        return skip();
    }

    private String generateFromPattern() {
        int min = coalesce(schema.getMinLength(), 0);
        int max = coalesce(schema.getMaxLength(), Integer.MAX_VALUE);
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = rgxGen.generate(random);
            if (candidate.length() >= min && candidate.length() <= max) {
                return candidate;
            }
        }
        return rgxGen.generate(random);
    }

    private String randomString() {
        int min = coalesce(schema.getMinLength(), 0);
        int max = coalesce(schema.getMaxLength(), min + 20);
        int length = min == max ? min : random.nextInt(min, max + 1);
        return randomStringOfLength(length);
    }

    private String randomStringOfLength(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
