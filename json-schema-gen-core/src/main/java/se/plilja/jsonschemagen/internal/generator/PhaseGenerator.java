package se.plilja.jsonschemagen.internal.generator;

abstract class PhaseGenerator<E extends Enum<E>, R> {

    private E phase;

    PhaseGenerator(Class<E> phaseClass) {
        this.phase = GenerationPhaseUtil.first(phaseClass);
    }

    R generate() {
        while (true) {
            GenerationResult<R> result = generatePhase(phase);
            E prev = phase;
            phase = advanceToNext(phase);
            if (result instanceof GenerationResult.Present<R> present) {
                return present.value();
            }
            if (prev == phase) {
                // We reached the end of the phases but were unable to generate any value
                throw new IllegalStateException("No applicable phase found");
            }
        }
    }

    protected E advanceToNext(E current) {
        return GenerationPhaseUtil.advanceToNext(current);
    }

    protected abstract GenerationResult<R> generatePhase(E phase);
}
