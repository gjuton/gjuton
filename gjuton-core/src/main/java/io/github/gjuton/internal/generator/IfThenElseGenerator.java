package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.generator.GenerationResult.result;

import io.github.gjuton.internal.model.Schema;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generator for schemas carrying {@code if}/{@code then}/{@code else}.
 *
 * <p>A schema may carry several conditionals, each independent. A value
 * satisfies one by matching its {@code if} and its {@code then}, or by failing
 * the {@code if} and satisfying its {@code else}. A generated value satisfies
 * every conditional the schema carries.
 */
final class IfThenElseGenerator extends PhaseGenerator<IfThenElseGenerator.GenerationPhase, Object> {

    private final SchemaValidator validator;
    private final Schema validationTarget;
    private final Schema base;
    private final List<Schema.Conditional> conditionals;

    enum GenerationPhase {
        THEN, ELSE, RANDOM
    }

    IfThenElseGenerator(GeneratorContext context, Schema parent) {
        super(GenerationPhase.class, context);
        this.validator = new SchemaValidator(context);
        this.validationTarget = parent;

        // Strip the conditional keywords so the branch schemas composed below
        // (and anything generated from base) don't re-dispatch back into this
        // generator; if/then/else is applied once, here.
        this.base = parent.toBuilder()
                .ifSchema(null)
                .thenSchema(null)
                .elseSchema(null)
                .additionalConditionals(null)
                .build();
        this.conditionals = parent.getConditionals();
    }

    /**
     * The schema a value must satisfy to take the given side of each conditional.
     * That an else-side also requires the {@code if} to fail is left to validating
     * the candidate rather than encoded here.
     *
     * @throws io.github.gjuton.errors.UnsatisfiableSchemaException if no value can
     *     take those sides
     */
    private Schema composition(boolean[] thenSides) {
        var branches = new ArrayList<Schema>();
        branches.add(base);
        for (int i = 0; i < conditionals.size(); i++) {
            var conditional = conditionals.get(i);
            if (thenSides[i]) {
                branches.add(conditional.ifSchema());
                if (conditional.thenSchema() != null) {
                    branches.add(conditional.thenSchema());
                }
            } else if (conditional.elseSchema() != null) {
                branches.add(conditional.elseSchema());
            }
        }
        return context.mergedSchema(branches);
    }

    /**
     * Draws a side per conditional — exactly one then-side, or each drawn on
     * its own. A lone conditional is always an even coin.
     */
    private boolean[] drawSides() {
        var random = context.random();
        var sides = new boolean[conditionals.size()];
        // One then-side is what a discriminator wants, where only one if may
        // match. With a lone conditional that is not a shape, only a bias.
        if (sides.length > 1 && random.nextBoolean()) {
            sides[random.nextInt(sides.length)] = true;
        } else {
            for (int i = 0; i < sides.length; i++) {
                sides[i] = random.nextBoolean();
            }
        }
        return sides;
    }

    /**
     * The random phase, the only one that can put conditionals on differing
     * sides. A schema whose conditionals exclude one another can satisfy no
     * other.
     */
    @Override
    protected GenerationPhase minimalPhase() {
        return GenerationPhase.RANDOM;
    }

    @Override
    protected GenerationResult<Object> generatePhase(GenerationPhase phase) {
        var sides = switch (phase) {
            case THEN -> {
                var all = new boolean[conditionals.size()];
                Arrays.fill(all, true);
                yield all;
            }
            case ELSE -> new boolean[conditionals.size()];
            case RANDOM -> drawSides();
        };
        var composed = composition(sides);
        var candidate = context.generatorFor(composed).generate();
        var violation = validator.violation(candidate, validationTarget);
        if (violation == null) {
            return result(candidate);
        }
        log.trace("discarding candidate for then-sides {}: {}", Arrays.toString(sides), violation);
        return GenerationResult.skip();
    }
}
