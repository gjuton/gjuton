package se.plilja.jsonschemagen.internal.generator;

import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.util.EnumUtil;

/**
 * Generator that walks through a sequence of named phases, producing
 * one value per {@link #generate()} call. Phases are tried in order;
 * a phase that cannot produce a value is skipped in favour of the next.
 */
public abstract class PhaseGenerator<E extends Enum<E>, R> implements Generator<R> {

    private static final int RETRY_BUDGET = 10;

    protected final GeneratorContext context;
    private E phase;

    protected PhaseGenerator(Class<E> phaseClass, GeneratorContext context) {
        this.context = context;
        this.phase = EnumUtil.first(phaseClass);
    }

    public R generate() {
        if (context.isMinimal()) {
            var result = generatePhase(minimalPhase());
            if (result instanceof GenerationResult.Present<R> present) {
                return present.value();
            }
            throw new IllegalStateException("Minimal phase must always produce a value");
        }
        UnsatisfiableSchemaException lastException = null;
        for (int attempt = 0; attempt < RETRY_BUDGET; attempt++) {
            GenerationResult<R> result;
            try {
                result = generatePhase(phase);
            } catch (UnsatisfiableSchemaException e) {
                lastException = e;
                result = GenerationResult.skip();
            }
            var prev = phase;
            phase = advanceToNext(phase);
            if (result instanceof GenerationResult.Present<R> present) {
                return present.value();
            }
        }
        throw lastException != null ? lastException
                : new UnsatisfiableSchemaException("Unable to generate a value satisfying the schema");
    }

    protected E advanceToNext(E current) {
        return EnumUtil.next(current);
    }

    /**
     * Returns the phase that would generate the smallest possible result satisfying
     * the constraints. Please note that this phase should not be skippable.
     */
    protected abstract E minimalPhase();

    protected abstract GenerationResult<R> generatePhase(E phase);
}
