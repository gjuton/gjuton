package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.generator.GenerationResult.result;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.BooleanSchema;
import io.github.gjuton.internal.model.NullSchema;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.StringSchema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import java.util.List;

/**
 * Generator for schemas with an {@code enum} keyword. An {@code enum}
 * restricts the value to a fixed set of allowed literals.
 *
 * <p>When combining keywords ({@code oneOf}, {@code anyOf}, {@code allOf},
 * {@code if}/{@code then}/{@code else}) accompany the {@code enum}, only
 * literals that also satisfy those keywords are produced. If no literal
 * satisfies the full schema, it is unsatisfiable.
 */
final class EnumGenerator extends PhaseGenerator<EnumGenerator.GenerationPhase, Object> {

    private final List<Object> values;
    private int index = 0;
    private int lastPickedIndex;

    enum GenerationPhase {
        EXHAUSTIVE, RANDOM
    }

    EnumGenerator(GeneratorContext context, List<Object> values, Schema validationTarget) {
        super(GenerationPhase.class, context);
        // Combining keywords further restrict the enum. Filter the finite
        // candidate set up front so no phase can emit an invalid literal;
        // generate-then-retry could spuriously exhaust when the valid
        // literals are a minority.
        var validator = new SchemaValidator(context);
        this.values = values.stream()
                .filter(value -> validator.satisfies(value, validationTarget))
                .toList();
        if (this.values.isEmpty()) {
            throw new UnsatisfiableSchemaException(unsatisfiableReason(values, validationTarget),
                    context.currentJsonPointer());
        }
    }

    /**
     * The failure to report when a schema admits none of its enum values.
     * Members that are all of one JSON type which the declared type does not
     * admit make the schema contradictory rather than merely over-constrained,
     * and the reason then names both types; any other cause is reported
     * without one.
     */
    private static String unsatisfiableReason(List<Object> values, Schema schema) {
        var reason = "No enum value satisfies the schema";
        var declaredType = switch (schema) {
            case StringSchema ignored -> "string";
            case NumericSchema numeric -> numeric.isInteger() ? "integer" : "number";
            case BooleanSchema ignored -> "boolean";
            case NullSchema ignored -> "null";
            case ObjectSchema ignored -> "object";
            case ArraySchema ignored -> "array";
            case UntypedSchema ignored -> null;
            case UnsatisfiableSchema ignored -> null;
        };
        if (declaredType == null) {
            return reason;
        }
        var memberTypes = values.stream().map(EnumGenerator::jsonType).distinct().toList();
        if (memberTypes.size() != 1) {
            return reason;
        }
        var memberType = memberTypes.getFirst();
        // An integer type admits numbers — 10 passes it — so numeric members of
        // one fail on a constraint rather than on the type they are.
        var admitted = memberType.equals(declaredType)
                || (memberType.equals("number") && declaredType.equals("integer"));
        if (admitted) {
            return reason;
        }
        return reason + ": the enum values are " + memberType + " but the schema declares type " + declaredType;
    }

    /**
     * The JSON Schema type name of a value as it appears in a parsed document.
     * Every number is {@code "number"} — {@code "integer"} is a constraint a
     * schema places on a number, not a type a value carries on its own.
     */
    private static String jsonType(Object value) {
        return switch (value) {
            case null -> "null";
            case Boolean ignored -> "boolean";
            case Number ignored -> "number";
            case String ignored -> "string";
            case List<?> ignored -> "array";
            // a JSON value that is none of the above is an object
            default -> "object";
        };
    }

    @Override
    protected GenerationPhase minimalPhase() {
        return GenerationPhase.EXHAUSTIVE;
    }

    @Override
    protected GenerationPhase advanceToNext(GenerationPhase current) {
        if (current == GenerationPhase.EXHAUSTIVE) {
            index++;
            if (index < values.size()) {
                return GenerationPhase.EXHAUSTIVE;
            }
        }
        return super.advanceToNext(current);
    }

    @Override
    protected GenerationResult<Object> generatePhase(GenerationPhase phase) {
        // index can outgrow values.size() when this instance is shared (via
        // GeneratorContext's identity cache) across more calls than it has values —
        // e.g. minimal mode always retries from EXHAUSTIVE without ever advancing
        // to RANDOM. Wrapping keeps it cycling through boundary values instead of
        // throwing.
        lastPickedIndex = switch (phase) {
            case EXHAUSTIVE -> index % values.size();
            case RANDOM -> context.random().nextInt(values.size());
        };
        return result(values.get(lastPickedIndex));
    }

    /**
     * The literal actually picked, rather than the phase — {@code RANDOM}
     * itself draws from the same finite literal set as {@code EXHAUSTIVE}, so
     * novelty must be tracked per literal, not per phase.
     */
    @Override
    protected int noveltyIndex(GenerationPhase phase) {
        return lastPickedIndex;
    }
}
