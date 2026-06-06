package se.plilja.jsonschemagen.internal.generator;

import static se.plilja.jsonschemagen.internal.generator.GenerationResult.result;

final class NullGenerator extends PhaseGenerator<NullGenerator.GenerationPhase, Object> {

    enum GenerationPhase {
        NULL
    }

    NullGenerator() {
        super(GenerationPhase.class);
    }

    @Override
    protected GenerationResult<Object> generatePhase(GenerationPhase phase) {
        return result(null);
    }
}
