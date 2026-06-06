package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import java.util.Random;
import se.plilja.jsonschemagen.internal.model.StringSchema;

final class StringGenerator extends PhaseGenerator<StringGenerator.GenerationPhase, String> {

    // TODO consider generating more tricky characters such as newlines <, > and so on
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

    private final Random random;

    enum GenerationPhase {
        EMPTY, RANDOM
    }

    StringGenerator(Random random, StringSchema schema) {
        super(GenerationPhase.class);
        this.random = random;
    }

    @Override
    protected GenerationResult<String> generatePhase(GenerationPhase phase) {
        return result(switch (phase) {
            case EMPTY -> "";
            case RANDOM -> randomString();
        });
    }

    private String randomString() {
        int length = random.nextInt(1, 21);
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
