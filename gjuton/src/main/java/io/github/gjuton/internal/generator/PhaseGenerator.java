package io.github.gjuton.internal.generator;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.util.EnumUtil;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator that walks through a sequence of named phases, producing
 * one value per {@link #generate()} call. Phases are tried in order;
 * a phase that cannot produce a value is skipped in favour of the next.
 */
public abstract class PhaseGenerator<E extends Enum<E>, R> implements Generator<R> {

    private static final int RETRY_BUDGET = 10;

    // Subclasses share this one rather than declaring their own, so a line
    // names the generator that ran, not the class that wrote it.
    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final GeneratorContext context;
    private E phase;

    protected PhaseGenerator(Class<E> phaseClass, GeneratorContext context) {
        this.context = context;
        this.phase = EnumUtil.first(phaseClass);
    }

    public R generate() {
        // Minimal and random-only modes both start from a fixed phase instead of
        // the shared phase field, and don't persist advances to it: they retry
        // locally (a candidate may skip, e.g. on post-generation validation
        // failure) rather than requiring first-attempt success, and the shared
        // cycling state belongs to the exhaustive cycle across separate
        // generate() calls. Minimal mode takes precedence so recursion still
        // terminates even when random-only is configured.
        UnsatisfiableSchemaException lastException = null;
        boolean cycling = !context.isMinimal() && !context.isRandomOnly();
        var candidate = startingPhase();
        // Counted rather than logged per attempt: one cause usually repeats
        // for the whole budget, burying the nested explanation of it.
        var failureCounts = new LinkedHashMap<String, Integer>();
        for (int attempt = 0; attempt < RETRY_BUDGET; attempt++) {
            var triedPhase = candidate;
            GenerationResult<R> result;
            boolean failed = false;
            try {
                result = attemptPhase(triedPhase);
            } catch (UnsatisfiableSchemaException e) {
                lastException = e;
                result = GenerationResult.skip();
                failed = true;
                failureCounts.merge(triedPhase + ": " + e.getMessage(), 1, Integer::sum);
            }
            candidate = advanceToNext(candidate);
            if (cycling) {
                phase = candidate;
            }
            if (result instanceof GenerationResult.Present<R> present) {
                return present.value();
            }
            if (!failed) {
                failureCounts.merge(triedPhase + ": produced no value", 1, Integer::sum);
            }
        }
        log.trace("giving up after {} attempts: {}", RETRY_BUDGET, failureCounts);
        throw lastException != null ? lastException
                : new UnsatisfiableSchemaException("Unable to generate a value satisfying the schema",
                        context.currentJsonPointer());
    }

    /**
     * Tries {@code candidatePhase}, registering it as visited on success.
     * A discarded candidate — whether declined or failed — leaves no trace
     * in the novelty state.
     */
    private GenerationResult<R> attemptPhase(E candidatePhase) {
        int mark = context.checkpoint();
        boolean succeeded = false;
        try {
            var result = generatePhase(candidatePhase);
            succeeded = result instanceof GenerationResult.Present<R>;
            if (succeeded) {
                context.registerVisit(this, noveltyIndex(candidatePhase));
            }
            return result;
        } finally {
            if (!succeeded) {
                context.rollback(mark);
            }
        }
    }

    private E startingPhase() {
        if (context.isMinimal()) {
            return minimalPhase();
        }
        if (context.isRandomOnly()) {
            return randomPhase();
        }
        return phase;
    }

    protected E advanceToNext(E current) {
        return EnumUtil.next(current);
    }

    /**
     * The novelty-tracking index for {@code phase}. Defaults to the phase's
     * declared ordinal, which is precise enough for generators whose every
     * phase — including the random one — emits a fixed, singular value.
     * Generators whose random phase itself draws from a finite set of
     * distinguishable outcomes (an enum literal, a branch) override this to
     * track that finer-grained outcome instead.
     */
    protected int noveltyIndex(E phase) {
        return phase.ordinal();
    }

    /**
     * Returns the phase tried first in minimal mode. May skip — unlike the
     * normal phase cycle, it is not required to always produce a value.
     */
    protected abstract E minimalPhase();

    /**
     * Returns the phase that emits a purely random value, used exclusively in
     * random-only mode. By convention this is the last declared phase, which
     * every generator uses as the terminal fallback of its boundary-value cycle.
     */
    protected E randomPhase() {
        return EnumUtil.last(phase.getDeclaringClass());
    }

    protected abstract GenerationResult<R> generatePhase(E phase);
}
