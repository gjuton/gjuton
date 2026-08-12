package io.github.gjuton.internal.generator;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.SchemaDocument;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Shared mutable state for a single generation run.
 *
 * <p>A single instance is created at the root and threaded through the
 * entire generator tree. It provides the shared random source, resolves
 * {@code $ref} targets, and ensures that generators reaching the same
 * schema definition share phase state rather than restarting independently.
 */
@Slf4j
public final class GeneratorContext {

    private static final int NOVELTY_WINDOW_SIZE = 5;

    /**
     * The allowance {@link #enterRetryLoop} grants the outermost round of
     * attempts, what it decays by for each round nested inside it, and the
     * floor it never falls under. The floor leaves every round enough attempts
     * to walk past a phase that declines.
     */
    private static final int MAX_RETRY_BUDGET = 10;
    private static final double RETRY_BUDGET_DECAY = 0.6;
    private static final int MIN_RETRY_BUDGET = 2;

    /**
     * Upper bound on {@link #mergedSchemaCache}'s size. Bounds memory for
     * schemas whose {@code anyOf}/{@code oneOf} random-subset picks can
     * produce a large number of distinct branch combinations over a long
     * generation run.
     */
    static final int MERGED_SCHEMA_CACHE_CAPACITY = 256;

    /**
     * The parsed document, used to look up {@code $ref} targets.
     */
    private final SchemaDocument document;

    private final Random random;

    private final GeneratorConfig config;

    /**
     * One {@link JsonGenerator} per {@link Schema} instance. Identity-keyed so
     * that all call sites reaching the same definition share phase state — the
     * boundary-value cycle advances globally instead of restarting per caller.
     */
    private final Map<Schema, JsonGenerator> generatorCache = new IdentityHashMap<>();

    /**
     * How deeply the position currently being generated sits inside the root
     * value: a property and an element each count one level, a {@code $ref}
     * none. Past the soft limit generators collapse to their smallest valid
     * form, bounding recursive optional structures.
     */
    private int nestingDepth;

    /**
     * How many rounds of attempts are under way at the position currently being
     * generated, which {@link #enterRetryLoop} rations against. Unrelated to
     * {@link #nestingDepth}: a level may cost several rounds or none.
     */
    private int retryLoopsUnderWay;

    /**
     * JSON path of the position currently being generated, e.g. {@code $.a[0]}.
     * Object and array generators extend it as they descend into a child and
     * restore it on the way back up, so {@link #currentPathOverride} can look up
     * an override registered for exactly this position.
     */
    private final StringBuilder currentPath = new StringBuilder("$");

    /**
     * Override values already produced during the current generation run, keyed
     * by path. A validate-and-retry parent may regenerate the same subtree
     * several times within one run; memoizing here keeps each override to a
     * single invocation per run and pins its value across those retries. Reset
     * by {@link #startRun}.
     */
    private final Map<String, Object> overridesThisRun = new HashMap<>();

    /**
     * Whether each of the most recent completed generation runs produced at
     * least one novel value, oldest first, capped at {@link #NOVELTY_WINDOW_SIZE}.
     */
    private final ArrayDeque<Boolean> noveltyWindow = new ArrayDeque<>();

    /**
     * Deliberate-value indices each generator has emitted at least once, across
     * all runs so far. Identity-keyed so generators sharing phase state (see
     * {@link #generatorCache}) also share their novelty history.
     */
    private final Map<Generator<?>, BitSet> noveltyBits = new IdentityHashMap<>();

    /**
     * Whether each of a generator's most recent completed visits committed a
     * deliberate value it had not already emitted, oldest first, capped at
     * {@link #NOVELTY_WINDOW_SIZE}. Identity-keyed so generators sharing phase
     * state (see {@link #generatorCache}) also share this history, mirroring
     * {@link #noveltyBits}. Unlike {@link #noveltyWindow}, a generator not
     * visited in a given run gets no entry for that run.
     */
    private final Map<Generator<?>, ArrayDeque<Boolean>> noveltyWindowByGenerator = new IdentityHashMap<>();

    /**
     * Merged schemas already computed, keyed by their input branch list, so
     * repeated merges of the same branches return the same {@link Schema}
     * instance. Capped at {@link #MERGED_SCHEMA_CACHE_CAPACITY}, least
     * recently used first; an eviction here also drops the evicted schema's
     * generator and novelty history so they don't outlive it.
     */
    private final Map<Set<Schema>, Schema> mergedSchemaCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Set<Schema>, Schema> eldest) {
            if (size() <= MERGED_SCHEMA_CACHE_CAPACITY) {
                return false;
            }
            var evictedGenerator = generatorCache.remove(eldest.getValue());
            if (evictedGenerator != null) {
                log.trace("the merge cache is full at {} entries: discarding a merged schema and its novelty history",
                        MERGED_SCHEMA_CACHE_CAPACITY);
                noveltyBits.remove(evictedGenerator.delegate());
                noveltyWindowByGenerator.remove(evictedGenerator.delegate());
            }
            return true;
        }
    };

    /**
     * Visits registered so far in the current run, in order, so a discarded
     * candidate's visits can be undone by {@link #rollback}.
     */
    private final List<VisitJournalEntry> visitJournal = new ArrayList<>();

    private record VisitJournalEntry(Generator<?> generator, int index) {
    }

    /**
     * A context for tests that only care about generated values, configured
     * for exhaustive generation with no overrides. Production code builds a
     * context from the caller's own {@link GeneratorConfig}.
     */
    static GeneratorContext testContext(SchemaDocument document, Random random) {
        return new GeneratorContext(document, random, GeneratorConfig.defaultExhaustive());
    }

    GeneratorContext(SchemaDocument document, Random random, GeneratorConfig config) {
        this.document = document;
        this.random = random;
        this.config = config;
    }

    public Random random() {
        return random;
    }

    /**
     * The bounds narrowing generated values: the mode's defaults, with anything
     * the caller constrained overlaid on top.
     */
    public ValueConstraints constraints() {
        return config.constraints();
    }

    /**
     * The bounds the caller set, null wherever they set none. Tells a bound the
     * caller chose, which is theirs to keep, from a mode default, which a
     * generator may widen.
     */
    public ValueConstraints callerConstraints() {
        return config.callerConstraints();
    }

    boolean isRandomOnly() {
        return config.randomOnly();
    }

    /**
     * Whether boundary-value phases run, on top of the random ones.
     */
    boolean isExhaustive() {
        return !config.randomOnly();
    }

    boolean generateAdditionalProperties() {
        return config.generateAdditionalProperties();
    }

    boolean isMinimal() {
        return nestingDepth >= config.softNestingDepth();
    }

    /**
     * Begins a round of attempts nested inside those already under way, and
     * answers how many it may spend — fewer the further in it sits, but never
     * below what walking past a declining phase takes. Must be paired with an
     * {@link #exitRetryLoop}; a call that throws needs no pairing.
     *
     * @throws UnsatisfiableSchemaException if the rounds under way outnumber
     *     the levels the hard limit allows, so some produced nothing
     */
    int enterRetryLoop() {
        int enclosingRounds = retryLoopsUnderWay;
        // Rounds outnumbering the levels allowed means one produced nothing —
        // recursion on the spot, which one more round never ends. Rounds merely
        // equalling them are ordinary generation, left for enterPath to refuse.
        if (enclosingRounds > config.hardNestingDepth()) {
            throw new UnsatisfiableSchemaException(
                    "Generating this schema keeps recursing without producing a value, past the configured nesting limit of "
                            + config.hardNestingDepth() + " levels",
                    currentJsonPointer());
        }
        retryLoopsUnderWay++;
        double decayed = MAX_RETRY_BUDGET * Math.pow(RETRY_BUDGET_DECAY, enclosingRounds);
        long rounded = Math.round(decayed);
        return (int) Math.max(MIN_RETRY_BUDGET, rounded);
    }

    /**
     * Ends the round of attempts begun by {@link #enterRetryLoop}, restoring
     * the allowance the next one is granted to what it was before.
     */
    void exitRetryLoop() {
        retryLoopsUnderWay--;
    }

    JsonGenerator generatorFor(Schema schema) {
        return generatorCache.computeIfAbsent(schema, s -> new JsonGenerator(s, this));
    }

    /**
     * Resets per-run generation state. Must be called once at the start of a
     * full generation run so that each registered override is consulted afresh
     * for that run.
     */
    void startRun() {
        overridesThisRun.clear();
        visitJournal.clear();
        MDC.put(GjutonMdc.PATH_KEY, currentPath.toString());
        MDC.put(GjutonMdc.NESTING_DEPTH_KEY, Integer.toString(nestingDepth));
        MDC.put(GjutonMdc.MODE_KEY, config.randomOnly() ? "RANDOM" : "EXHAUSTIVE");
    }

    /**
     * Records that {@code generator} visited its deliberate value at
     * {@code index}.
     */
    void registerVisit(Generator<?> generator, int index) {
        visitJournal.add(new VisitJournalEntry(generator, index));
    }

    /**
     * Marks the current point in the run's visit history, to later
     * {@link #rollback} to if the candidate being generated is discarded.
     */
    int checkpoint() {
        return visitJournal.size();
    }

    /**
     * Discards every visit registered since {@code mark} was taken, as if
     * they had never happened.
     */
    void rollback(int mark) {
        visitJournal.subList(mark, visitJournal.size()).clear();
    }

    /**
     * Finalizes the current generation run, updating per-generator and global
     * novelty scores based on the visits registered since {@link #startRun}.
     * Must be called once at the end of every full generation run.
     */
    void completeRun() {
        boolean runHasNovelty = false;
        var novelByGenerator = new IdentityHashMap<Generator<?>, Boolean>();
        for (var entry : visitJournal) {
            var bits = noveltyBits.computeIfAbsent(entry.generator(), ignored -> new BitSet());
            boolean isNovel = !bits.get(entry.index());
            if (isNovel) {
                bits.set(entry.index());
                runHasNovelty = true;
            }
            novelByGenerator.merge(entry.generator(), isNovel, Boolean::logicalOr);
        }
        novelByGenerator.forEach((generator, isNovel) -> {
            var window = noveltyWindowByGenerator.computeIfAbsent(generator, ignored -> new ArrayDeque<>());
            if (window.size() == NOVELTY_WINDOW_SIZE) {
                window.removeFirst();
            }
            window.addLast(isNovel);
        });
        if (noveltyWindow.size() == NOVELTY_WINDOW_SIZE) {
            noveltyWindow.removeFirst();
        }
        noveltyWindow.addLast(runHasNovelty);
        visitJournal.clear();
    }

    /**
     * Merges {@code schemas} into one, returning the same {@link Schema}
     * instance for equal lists so callers that merge the same combination
     * repeatedly (per-call branch selection in {@code oneOf}/{@code anyOf})
     * get back a schema whose generator and novelty history persist across
     * calls instead of restarting from scratch every time.
     */
    Schema mergedSchema(List<Schema> schemas) {
        if (schemas.size() == 1) {
            return schemas.get(0);
        }
        var key = Set.copyOf(schemas);
        // get()/put() rather than computeIfAbsent(): access-order LRU eviction
        // only reorders on get(), not on a computeIfAbsent cache hit.
        var cached = mergedSchemaCache.get(key);
        if (cached != null) {
            return cached;
        }
        var merged = SchemaMerger.merge(this, schemas, null, currentJsonPointer());
        mergedSchemaCache.put(key, merged);
        return merged;
    }

    /**
     * The fraction of the most recent completed generation runs (up to
     * {@link #NOVELTY_WINDOW_SIZE}) that produced at least one value not
     * already emitted by an earlier run. {@code 1.0} before any run has
     * completed.
     */
    public double noveltyScore() {
        if (noveltyWindow.isEmpty()) {
            return 1.0;
        }
        long novelRuns = noveltyWindow.stream().filter(Boolean::booleanValue).count();
        return (double) novelRuns / noveltyWindow.size();
    }

    /**
     * The fraction of {@code generator}'s own most recent completed visits
     * (up to {@link #NOVELTY_WINDOW_SIZE}) that committed a deliberate value
     * it had not already emitted. Empty if {@code generator} has never been
     * visited in a completed run.
     */
    Optional<Double> noveltyScore(Generator<?> generator) {
        var window = noveltyWindowByGenerator.get(generator);
        if (window == null || window.isEmpty()) {
            return Optional.empty();
        }
        long novelRuns = window.stream().filter(Boolean::booleanValue).count();
        return Optional.of((double) novelRuns / window.size());
    }

    /**
     * Returns the caller's override for the position at the current path, or
     * {@code null} if no override is registered there.
     *
     * <p>Path-based overrides are checked first; if none matches and the current
     * position is an object property (not an array element or the root),
     * name-based overrides are checked against the property name.
     *
     * <p>Within one run (see {@link #startRun}) an override is consulted at most
     * once per memoization key. Path-based overrides are keyed by path, so
     * retries at the same position see the same value. Name-based overrides are
     * keyed by property name, so every position with the same name shares one
     * value per run — the property means the same thing wherever it appears.
     */
    Object currentPathOverride() {
        var path = currentPath.toString();
        var override = config.pathOverrides().get(path);
        if (override != null) {
            return overridesThisRun.computeIfAbsent(path, ignored -> new OverriddenValue(override.get()));
        }

        // The path string doesn't distinguish position kinds (object property vs
        // array element vs root), so recover that from its shape: paths ending
        // with ']' are array elements, paths with no '.' are the root — only the
        // rest are object properties where name-based matching applies.
        if (!config.nameOverrides().isEmpty() && path.charAt(path.length() - 1) != ']') {
            int lastDot = path.lastIndexOf('.');
            if (lastDot >= 0) {
                var propertyName = path.substring(lastDot + 1);
                var nameOverride = config.nameOverrides().get(propertyName);
                if (nameOverride != null) {
                    return overridesThisRun.computeIfAbsent(
                            propertyName, ignored -> new OverriddenValue(nameOverride.get()));
                }
            }
        }

        return null;
    }

    /**
     * The caller's override for strings carrying {@code format}, or
     * {@code null} when none is registered. Matches the format as written,
     * including ones gjuton does not model.
     */
    Supplier<Object> formatOverride(String format) {
        return format == null ? null : config.formatOverrides().get(format);
    }

    /**
     * The current generation position as a JSON Pointer (RFC 6901),
     * e.g. {@code /address/street}. Returns the empty string at the
     * root.
     */
    public String currentJsonPointer() {
        var jsonPath = currentPath.toString();
        if ("$".equals(jsonPath)) {
            return "";
        }
        return jsonPath.substring(1)
                .replace('.', '/')
                .replace('[', '/')
                .replace("]", "");
    }

    /**
     * Descends one level, into the child reached by appending
     * {@code pathSegment} (e.g. {@code ".name"} or {@code "[0]"}). Must be
     * paired with an {@link #exitPath} for the same segment; a call that throws
     * needs no pairing.
     *
     * @throws UnsatisfiableSchemaException if the child would nest deeper than
     *     the hard limit allows
     */
    void enterPath(String pathSegment) {
        if (nestingDepth >= config.hardNestingDepth()) {
            throw new UnsatisfiableSchemaException(
                    "Generating this schema needs more than the configured nesting limit of " + config.hardNestingDepth()
                            + " levels of objects and arrays",
                    currentJsonPointer());
        }
        currentPath.append(pathSegment);
        nestingDepth++;
        MDC.put(GjutonMdc.PATH_KEY, currentPath.toString());
        MDC.put(GjutonMdc.NESTING_DEPTH_KEY, Integer.toString(nestingDepth));
    }

    /**
     * Ascends out of the child entered with {@link #enterPath}, restoring the
     * path to what it was before. {@code pathSegment} must match the paired
     * {@link #enterPath} call.
     */
    void exitPath(String pathSegment) {
        currentPath.setLength(currentPath.length() - pathSegment.length());
        nestingDepth--;
        MDC.put(GjutonMdc.PATH_KEY, currentPath.toString());
        MDC.put(GjutonMdc.NESTING_DEPTH_KEY, Integer.toString(nestingDepth));
    }

    /**
     * Resolves a {@code $ref} string to the {@link Schema} it points at.
     *
     * @throws IllegalArgumentException if the ref cannot be resolved
     */
    Schema resolveRef(String ref) {
        var target = document.resolveRef(ref);
        if (target == null) {
            throw new IllegalArgumentException("Unresolved $ref: " + ref);
        }
        return target;
    }

}
