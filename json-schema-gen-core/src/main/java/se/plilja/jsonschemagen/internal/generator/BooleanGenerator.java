package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import java.util.Random;

final class BooleanGenerator extends PhaseGenerator<BooleanGenerator.GenerationPhase, Boolean> {

    private final Random random;

    enum GenerationPhase {
        FALSE, TRUE, RANDOM
    }

    BooleanGenerator(Random random) {
        super(GenerationPhase.class);
        this.random = random;
    }

    @Override
    protected GenerationResult<Boolean> generatePhase(GenerationPhase phase) {
        return result(switch (phase) {
            case FALSE -> false;
            case TRUE -> true;
            case RANDOM -> random.nextBoolean();
        });
    }
}
