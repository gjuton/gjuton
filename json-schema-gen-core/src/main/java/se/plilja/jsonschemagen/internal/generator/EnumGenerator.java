package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

import java.util.List;
import java.util.Random;

final class EnumGenerator extends PhaseGenerator<EnumGenerator.GenerationPhase, Object> {

    private final Random random;
    private final List<Object> values;
    private int index = 0;

    enum GenerationPhase {
        EXHAUSTIVE, RANDOM
    }

    EnumGenerator(Random random, List<Object> values) {
        super(GenerationPhase.class);
        this.random = random;
        this.values = values;
    }

    @Override
    protected GenerationPhase advanceToNext(GenerationPhase current) {
        if (current == GenerationPhase.EXHAUSTIVE) {
            index++;
            if (index < values.size()) {
                return GenerationPhase.EXHAUSTIVE;
            }
        }
        return super.advanceToNext(current);
    }

    @Override
    protected GenerationResult<Object> generatePhase(GenerationPhase phase) {
        return result(switch (phase) {
            case EXHAUSTIVE -> values.get(index);
            case RANDOM -> values.get(random.nextInt(values.size()));
        });
    }
}
