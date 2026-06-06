package se.plilja.jsonschemagen.internal.generator;

import java.util.Optional;

abstract class PhaseGenerator<E extends Enum<E>, R> {

    private E phase;

    PhaseGenerator(Class<E> phaseClass) {
        this.phase = GenerationPhaseUtil.first(phaseClass);
    }

    R generate() {
        while (true) {
            Optional<R> result = generatePhase(phase);
            E prev = phase;
            phase = GenerationPhaseUtil.advanceToNext(phase);
            if (result.isPresent()) {
                return result.get();
            }
            if (prev == phase) {
                // We reached the end of the phases but were unable to generate any value
                throw new IllegalStateException("No applicable phase found");
            }
        }
    }

    protected abstract Optional<R> generatePhase(E phase);
}
