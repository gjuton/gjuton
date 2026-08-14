package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.generator.GenerationResult.result;
import static io.github.gjuton.internal.generator.GenerationResult.skip;
import static io.github.gjuton.internal.util.FunctionalUtil.coalesce;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.NullSchema;
import io.github.gjuton.internal.model.Schema;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generator for {@code "type": "array"} schemas. Varies array length
 * across successive calls to cover the allowed range.
 */
final class ArrayGenerator extends PhaseGenerator<ArrayGenerator.GenerationPhase, List<Object>> {

    /**
     * Extra length past {@code minItems} when the schema has no
     * {@code maxItems}. Gives the generator headroom to produce
     * arrays of varying lengths across phases.
     */
    private static final int DEFAULT_LENGTH_BUFFER = 5;

    /**
     * Retry budget for finding a distinct element when {@code uniqueItems}
     * is set, before giving up as unsatisfiable.
     */
    private static final int UNIQUE_ITEMS_RETRY_BUDGET = 20;

    private final ArraySchema schema;
    private final List<Schema> prefixSchemas;
    private final List<Schema> containsSchemas;
    private final Schema itemSchema;
    private final boolean additionalItemsAllowed;
    private final SchemaValidator validator;

    /**
     * How many elements every {@code contains} clause needs. Zero when
     * {@code minContains} relaxes the clauses to demanding nothing.
     */
    private final int requiredMatchesPerClause;

    private boolean reportedDefaultMaximumEffect;

    enum GenerationPhase {
        MIN_LENGTH, MAX_LENGTH, RANDOM
    }

    ArrayGenerator(GeneratorContext context, ArraySchema schema) {
        super(GenerationPhase.class, context);
        this.schema = schema;
        this.prefixSchemas = schema.getPrefixSchemas();
        this.containsSchemas = coalesce(schema.getContains(), List.of());
        // TODO: when items is absent, JSON Schema allows any element type. Emitting nulls is valid
        // but boring; a varied-type generator (cycling string/int/bool/...) would surface more bugs.
        this.itemSchema = coalesce(schema.getItemSchema(), new NullSchema());
        this.additionalItemsAllowed = schema.areAdditionalItemsAllowed();
        this.validator = new SchemaValidator(context);
        this.requiredMatchesPerClause = coalesce(schema.getMinContains(), 1);
    }

    @Override
    protected GenerationPhase minimalPhase() {
        return GenerationPhase.MIN_LENGTH;
    }

    @Override
    protected GenerationResult<List<Object>> generatePhase(GenerationPhase phase) {
        Integer maxContains = schema.getMaxContains();
        if (!containsSchemas.isEmpty() && maxContains != null && requiredMatchesPerClause > maxContains) {
            throw new UnsatisfiableSchemaException(
                    "minContains " + requiredMatchesPerClause + " exceeds maxContains " + maxContains,
                    context.currentJsonPointer());
        }
        int minLength = effectiveMinLength();
        int effectiveMax = effectiveMaxLength();
        if (effectiveMax < minLength) {
            throw new UnsatisfiableSchemaException(
                    "No valid array length satisfies minItems/maxItems/contains/minContains together: effective minimum length "
                            + minLength + " exceeds effective maximum length " + effectiveMax,
                    context.currentJsonPointer());
        }
        if (context.callerConstraints().arrayMaxLength() == null && !reportedDefaultMaximumEffect) {
            int defaultMax = context.constraints().arrayMaxLength();
            Integer schemaMax = schema.getMaxItems();
            if (minLength > defaultMax) {
                reportedDefaultMaximumEffect = true;
                log.info("{}: generating arrays of {} elements, past gjuton's default maximum of {}, to meet the minimum length",
                        context.currentJsonPointer(), minLength, defaultMax);
            } else if (context.isExhaustive() && schemaMax != null && schemaMax > defaultMax) {
                reportedDefaultMaximumEffect = true;
                log.info("{}: limiting arrays to gjuton's default maximum of {} elements, not the schema's maxItems of {};"
                                + " set Constraints.arrayLength to choose your own",
                        context.currentJsonPointer(), defaultMax, schemaMax);
            }
        }
        int length = switch (phase) {
            case MIN_LENGTH -> minLength;
            case MAX_LENGTH -> effectiveMax;
            case RANDOM -> minLength + context.random().nextInt(effectiveMax - minLength + 1);
        };
        var value = buildList(length);
        if (maxContains != null) {
            // Nothing keeps an ordinary element from matching a clause too, so the only way to
            // stay under the maximum is to decline a candidate that went over and draw again.
            var violation = validator.violation(value, schema);
            if (violation != null) {
                log.trace("{}: discarding array of {} elements: {}", context.currentJsonPointer(), length, violation);
                return skip();
            }
        }
        return result(value);
    }

    /**
     * The shortest array this generator produces: long enough for the schema's
     * minimum and the caller's. The schema's is either stated outright or
     * follows from its other constraints.
     */
    private int effectiveMinLength() {
        int minLength = coalesce(schema.getMinItems(), 0);
        int minInForce = context.constraints().arrayMinLength();
        minLength = Math.max(minLength, minInForce);
        int required = containsSchemas.size() * requiredMatchesPerClause;
        // prefixItems on its own requires no elements, so only clause matches
        // push the length past the prefix.
        if (required > 0) {
            minLength = Math.max(minLength, prefixSchemas.size() + required);
        }
        return minLength;
    }

    /**
     * The longest array this generator produces: the tighter of the schema's
     * {@code maxItems} and the maximum in force, capped at the tuple prefix when
     * {@code additionalItems} is false, and a fixed span past the minimum when
     * neither bounds it.
     */
    private int effectiveMaxLength() {
        int minLength = effectiveMinLength();
        Integer callerMax = context.callerConstraints().arrayMaxLength();
        int limit = context.constraints().arrayMaxLength();
        if (callerMax == null) {
            // A caller's maximum is honored even below the minimum length; a default
            // gives way, so the schema's minimum is still reachable.
            limit = Math.max(limit, minLength);
        }
        Integer schemaMax = schema.getMaxItems();
        int maxLength;
        if (schemaMax != null) {
            maxLength = Math.min(schemaMax, limit);
        } else if (callerMax == null) {
            maxLength = Math.min(minLength + DEFAULT_LENGTH_BUFFER, limit);
        } else {
            maxLength = limit;
        }
        if (!additionalItemsAllowed) {
            maxLength = Math.min(maxLength, prefixSchemas.size());
        }
        return maxLength;
    }

    private List<Object> buildList(int length) {
        var containsPositions = assignContainsPositions(length);
        var list = new ArrayList<>();
        if (schema.isUniqueItems()) {
            var seen = new HashSet<>();
            for (int i = 0; i < length; i++) {
                var element = generateDistinctElementAt(i, containsPositions, seen);
                list.add(element);
                seen.add(element);
            }
        } else {
            for (int i = 0; i < length; i++) {
                list.add(generateElementAt(i, containsPositions));
            }
        }
        return list;
    }

    private Object generateElementAt(int index, Map<Integer, Schema> containsPositions) {
        var segment = "[" + index + "]";
        return JsonGenerator.generateForPath(context, segment, () -> {
            if (containsPositions.containsKey(index)) {
                return containsPositions.get(index);
            } else if (index < prefixSchemas.size()) {
                return prefixSchemas.get(index);
            } else {
                return itemSchema;
            }
        });
    }

    /**
     * Generates the element at {@code index}, retrying on collision with an
     * already-placed element ({@code seen}) until a distinct value is found.
     *
     * @throws UnsatisfiableSchemaException if no distinct element can be
     *         produced within the retry budget
     */
    private Object generateDistinctElementAt(int index, Map<Integer, Schema> containsPositions, Set<Object> seen) {
        for (int attempt = 0; attempt < UNIQUE_ITEMS_RETRY_BUDGET; attempt++) {
            var element = generateElementAt(index, containsPositions);
            if (!seen.contains(element)) {
                return element;
            }
        }
        throw new UnsatisfiableSchemaException(
                "Could not generate a distinct element satisfying uniqueItems within the retry budget",
                context.currentJsonPointer());
    }

    /**
     * Maps array positions to the {@code contains} clause each one has to
     * satisfy. Every clause gets as many positions as it needs, no two clauses
     * share one, and the tuple prefix is left untouched.
     *
     * <p>{@code length} must leave room past the prefix for every element the
     * clauses need, which {@link #effectiveMinLength()} guarantees.
     */
    private Map<Integer, Schema> assignContainsPositions(int length) {
        int required = containsSchemas.size() * requiredMatchesPerClause;
        if (required == 0) {
            return Map.of();
        }
        var candidates = new ArrayList<Integer>();
        for (int i = prefixSchemas.size(); i < length; i++) {
            candidates.add(i);
        }
        Collections.shuffle(candidates, context.random());
        var positions = new HashMap<Integer, Schema>();
        for (int i = 0; i < required; i++) {
            int position = candidates.get(i);
            var clause = containsSchemas.get(i % containsSchemas.size());
            positions.put(position, clause);
        }
        return positions;
    }
}
