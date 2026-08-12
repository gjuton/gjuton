package io.github.gjuton.internal.generator;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Generation options that alter how values are produced, threaded from the
 * public API into the generator tree via {@link GeneratorContext}.
 *
 * <p>This is the internal counterpart of the public configuration surface; it
 * carries only the primitive values the generators need, so the generator
 * package stays free of {@code api} types.
 *
 * @param randomOnly                    emit only random values, skipping the
 *                                      boundary-value cycle
 * @param generateAdditionalProperties  add random extra properties to objects
 *                                      wherever the schema permits them
 * @param softNestingDepth              nesting depth at which generation
 *                                      collapses to minimal form
 * @param hardNestingDepth              nesting depth beyond which the schema
 *                                      is treated as unsatisfiable
 * @param pathOverrides                 value overrides keyed by JSON path; the
 *                                      supplier at a path yields a ready
 *                                      value-tree node to place there instead
 *                                      of generating one
 * @param nameOverrides                 value overrides keyed by property name;
 *                                      applied at every position whose property
 *                                      name matches, unless a path-based
 *                                      override already covers that position
 * @param formatOverrides               value overrides keyed by {@code format}
 *                                      as written; applied at every string
 *                                      carrying it
 * @param constraints                   the bounds in force: the mode's defaults
 *                                      with the caller's overlaid on them
 * @param callerConstraints             the caller's own bounds
 */
public record GeneratorConfig(
        boolean randomOnly,
        boolean generateAdditionalProperties,
        int softNestingDepth,
        int hardNestingDepth,
        Map<String, Supplier<Object>> pathOverrides,
        Map<String, Supplier<Object>> nameOverrides,
        Map<String, Supplier<Object>> formatOverrides,
        ValueConstraints constraints,
        ValueConstraints callerConstraints) {

    /**
     * The nesting-depth ceilings for the three presets the public API exposes,
     * in levels of objects and arrays. At the soft one structures collapse to
     * their smallest valid form; past the hard one generation fails rather than
     * emit a value missing a required property.
     */
    public static final int DEFAULT_SOFT_NESTING_DEPTH = 3;
    public static final int DEFAULT_HARD_NESTING_DEPTH = 10;
    public static final int SHALLOW_SOFT_NESTING_DEPTH = 1;
    public static final int SHALLOW_HARD_NESTING_DEPTH = 10;
    public static final int DEEP_SOFT_NESTING_DEPTH = 5;
    public static final int DEEP_HARD_NESTING_DEPTH = 13;

    public GeneratorConfig {
        pathOverrides = Map.copyOf(pathOverrides);
        nameOverrides = Map.copyOf(nameOverrides);
        formatOverrides = Map.copyOf(formatOverrides);
    }

    /**
     * A test fixture: exhaustive boundary-value generation, no synthesized
     * extra properties, no value overrides, and the default nesting depth
     * limits. Not the configuration a caller who sets no options gets — that
     * one is built from {@code GenerationMode.RANDOM}.
     */
    static GeneratorConfig defaultExhaustive() {
        return new GeneratorConfig(
                false,
                false,
                DEFAULT_SOFT_NESTING_DEPTH,
                DEFAULT_HARD_NESTING_DEPTH,
                Map.of(),
                Map.of(),
                Map.of(),
                ValueConstraints.forExhaustive(),
                ValueConstraints.none());
    }
}
