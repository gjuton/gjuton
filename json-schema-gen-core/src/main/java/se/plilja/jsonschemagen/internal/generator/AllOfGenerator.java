package se.plilja.jsonschemagen.internal.generator;

import java.util.ArrayList;
import se.plilja.jsonschemagen.internal.model.Schema;

/**
 * Generator for schemas with an {@code allOf} keyword. Generates values
 * satisfying all branches simultaneously.
 */
final class AllOfGenerator implements Generator<Object> {

    private final GeneratorContext context;
    private final Schema merged;

    AllOfGenerator(GeneratorContext context, Schema parent) {
        this.context = context;
        if (parent.getAllOf().isEmpty()) {
            throw new IllegalArgumentException("allOf must contain at least one sub-schema");
        }
        var branches = new ArrayList<Schema>();
        for (var branch : parent.getAllOf()) {
            if (branch.getRef() != null) {
                var resolved = context.resolveRef(branch.getRef());
                // Identity check: the parser reuses the same Schema instance for "#"
                if (resolved == parent) {
                    throw new IllegalArgumentException(
                            "Self-referential $ref '" + branch.getRef() + "' inside allOf is not supported");
                }
                branches.add(resolved);
            } else {
                branches.add(branch);
            }
        }
        branches.add(parent.toBuilder().allOf(null).build());
        this.merged = SchemaMerger.merge(branches);
    }

    @Override
    public Object generate() {
        return context.generatorFor(merged).generate();
    }
}
