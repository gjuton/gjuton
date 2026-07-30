package io.github.gjuton.internal.generator;

import org.slf4j.MDC;

/**
 * The MDC keys gjuton's trace output is labelled with. No key outlives the
 * generation run that set it.
 */
public final class GjutonMdc {

    /**
     * Identifies the generation run a line came from. A value already present
     * when a run starts is the caller's own, and stands in place of one of
     * gjuton's for that run.
     */
    public static final String RUN_ID_KEY = "gjutonRunId";

    /**
     * Identifies the position in the generated document a line concerns.
     */
    public static final String PATH_KEY = "gjutonPath";

    /**
     * How many {@code $ref} expansions stand between the root schema and the
     * position a line concerns.
     */
    public static final String REF_DEPTH_KEY = "gjutonRefDepth";

    /**
     * The {@code $ref} expansions enclosing the position a line concerns,
     * outermost first, naming where in the schema its subject is defined.
     * Only named references appear; {@link #REF_DEPTH_KEY} also counts
     * {@code allOf} resolution, so it may exceed the chain's length.
     */
    public static final String REF_CHAIN_KEY = "gjutonRefChain";

    /**
     * The seed the run generated from, prefixed {@code supplied-} when the
     * caller chose it and {@code random-} when gjuton did. Reproducing the
     * run needs nothing beyond this value.
     */
    public static final String SEED_KEY = "gjutonSeed";

    /**
     * The strategy the run was configured with, {@code RANDOM} or
     * {@code EXHAUSTIVE} — what the whole run was asked for, as opposed to
     * the phase a single generator stood at.
     */
    public static final String MODE_KEY = "gjutonMode";

    private GjutonMdc() {
    }

    /**
     * Removes every gjuton key from the MDC, including a run id the caller
     * supplied rather than gjuton itself. Keys belonging to anyone else are
     * left untouched.
     */
    public static void clear() {
        MDC.remove(RUN_ID_KEY);
        MDC.remove(PATH_KEY);
        MDC.remove(REF_DEPTH_KEY);
        MDC.remove(REF_CHAIN_KEY);
        MDC.remove(SEED_KEY);
        MDC.remove(MODE_KEY);
    }
}
