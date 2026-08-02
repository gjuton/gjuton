package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.util.CollectionUtil.concat;
import static io.github.gjuton.internal.util.FunctionalUtil.coalesce;
import static io.github.gjuton.internal.util.MathUtil.maxNullable;
import static io.github.gjuton.internal.util.MathUtil.minNullable;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.StringSchema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import io.github.gjuton.internal.util.MathUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Combines multiple schemas into one by taking the intersection of their
 * constraints. Used by {@code allOf} (all branches must hold) and by
 * {@code anyOf}/{@code oneOf} when parent-level sibling constraints need
 * to be folded into each branch.
 */
@Slf4j
final class SchemaMerger {

    private SchemaMerger() {
    }

    /**
     * Merges a list of schemas pairwise from left to right, producing a
     * single schema whose constraints are the intersection of all inputs.
     *
     * @throws IllegalArgumentException      if the list is empty
     * @throws UnsatisfiableSchemaException   if any pair has incompatible types or constraints
     */
    static Schema merge(List<Schema> schemas) {
        return merge(schemas, null, null);
    }

    /**
     * Like {@link #merge(List)}, but conflict exceptions are enriched with
     * the given {@code locationList} and {@code schemaPath}.
     *
     * @param locationList JSON Pointers identifying each schema's origin
     *                   (e.g. {@code /allOf/0}), or {@code null} to omit
     * @param schemaPath position in the generated document
     *                   (e.g. {@code /address}), or {@code null} to omit
     */
    static Schema merge(List<Schema> schemas, List<String> locationList, String schemaPath) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("merge requires at least one schema");
        }
        var locations = buildLocations(locationList, schemaPath);
        var result = schemas.get(0);
        for (int i = 1; i < schemas.size(); i++) {
            result = mergeTwoSchemas(result, schemas.get(i), locations);
        }
        return result;
    }

    private static Supplier<String> buildLocations(List<String> locationList, String schemaPath) {
        if (locationList == null && schemaPath == null) {
            return () -> "";
        }
        return () -> {
            var sb = new StringBuilder(" ");
            if (locationList != null) {
                var nonEmpty = locationList.stream()
                        .filter(loc -> loc != null && !loc.isEmpty())
                        .toList();
                sb.append("(merging ").append(String.join(", ", nonEmpty));
                if (schemaPath != null && !schemaPath.isEmpty()) {
                    sb.append(" at ").append(schemaPath);
                }
                sb.append(")");
            } else if (schemaPath != null && !schemaPath.isEmpty()) {
                sb.append("(at ").append(schemaPath).append(")");
            }
            return sb.toString();
        };
    }

    /**
     * Merges each schema in {@code schemas} with {@code other}, returning
     * only the compatible results. Schemas that are incompatible with
     * {@code other} are omitted from the returned list without failing the
     * merge, so the returned list may be shorter than {@code schemas} and
     * nothing else signals that to the caller.
     */
    static List<Schema> mergeEachWith(List<Schema> schemas, Schema other) {
        var result = new ArrayList<Schema>();
        for (int i = 0; i < schemas.size(); i++) {
            try {
                result.add(merge(List.of(schemas.get(i), other)));
            } catch (UnsatisfiableSchemaException incompatible) {
                log.trace("dropping branch {}: incompatible with the parent schema: {}", i, incompatible.getMessage());
            }
        }
        return result;
    }

    private static Schema mergeTwoSchemas(Schema a, Schema b, Supplier<String> locations) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        Schema merged;
        if (a instanceof UnsatisfiableSchema || b instanceof UnsatisfiableSchema) {
            merged = new UnsatisfiableSchema();
        } else if (a instanceof UntypedSchema && b instanceof UntypedSchema) {
            // Two untyped schemas have no type constraint to merge, so keeping the
            // left side alone would drop the right side's $ref. Prefer whichever
            // side actually carries one.
            merged = (a.getRef() == null && b.getRef() != null)
                    ? b.toBuilder().build()
                    : a.toBuilder().build();
        } else if (b instanceof UntypedSchema) {
            merged = a.toBuilder().build();
        } else if (a instanceof UntypedSchema) {
            merged = b.toBuilder().build();
        } else if (a instanceof StringSchema sa && b instanceof StringSchema sb) {
            merged = mergeStringSchemas(sa, sb, locations);
        } else if (a instanceof NumericSchema na && b instanceof NumericSchema nb) {
            merged = mergeNumericSchemas(na, nb, locations);
        } else if (a instanceof ObjectSchema oa && b instanceof ObjectSchema ob) {
            merged = mergeObjectSchemas(oa, ob, locations);
        } else if (a instanceof ArraySchema aa && b instanceof ArraySchema ab) {
            merged = mergeArraySchemas(aa, ab, locations);
        } else {
            throw new UnsatisfiableSchemaException(
                    "Cannot merge types " + typeName(a) + " and " + typeName(b) + locations.get());
        }

        var constValue = mergeConstValues(a.getConstValue(), b.getConstValue(), locations);
        var enumValues = mergeEnumValues(a.getEnumValues(), b.getEnumValues(), locations);
        if (constValue != null && enumValues != null && !enumValues.contains(constValue)) {
            throw new UnsatisfiableSchemaException(
                    "const value " + constValue + " is not in enum " + enumValues + locations.get());
        }
        var builder = merged.toBuilder()
                .constValue(constValue)
                .enumValues(enumValues)
                .oneOf(concat(a.getOneOf(), b.getOneOf()))
                .anyOf(concat(a.getAnyOf(), b.getAnyOf()))
                .allOf(concat(a.getAllOf(), b.getAllOf()));
        mergeConditional(builder, a, b);
        return builder.build();
    }

    /**
     * Carries every {@code if}/{@code then}/{@code else} conditional from
     * both sides through the merge. Each conditional must hold independently
     * in the merged schema, the same as {@code allOf} branches.
     */
    private static void mergeConditional(Schema.SchemaBuilder<?, ?> builder, Schema a, Schema b) {
        var conditionals = concat(a.getConditionals(), b.getConditionals());
        builder.ifSchema(null)
                .thenSchema(null)
                .elseSchema(null)
                .additionalConditionals(conditionals.isEmpty() ? null : conditionals);
    }

    /**
     * Merges two string schemas by tightening length bounds and combining
     * format constraints. Conflicting patterns keep the left side's;
     * conflicting formats throw {@link UnsatisfiableSchemaException}.
     */
    private static StringSchema mergeStringSchemas(StringSchema a, StringSchema b, Supplier<String> locations) {
        if (a.getRawFormat() != null && b.getRawFormat() != null && !a.getRawFormat().equals(b.getRawFormat())) {
            throw new UnsatisfiableSchemaException(
                    "Conflicting format constraints: " + a.getRawFormat() + " vs " + b.getRawFormat() + locations.get());
        }
        var minLength = maxNullable(a.getMinLength(), b.getMinLength());
        var maxLength = minNullable(a.getMaxLength(), b.getMaxLength());
        if (minLength != null && maxLength != null && minLength > maxLength) {
            throw new UnsatisfiableSchemaException(
                    "String length range is empty: minLength " + minLength
                            + " exceeds maxLength " + maxLength + locations.get());
        }
        if (a.getPattern() != null && b.getPattern() != null && !a.getPattern().equals(b.getPattern())) {
            // Not a warning: the merge may belong to an anyOf branch that is
            // discarded anyway, so the dropped pattern often costs nothing.
            log.trace("merging regex patterns is not supported, keeping '{}' and dropping '{}'{}: "
                    + "generated values may violate the dropped pattern",
                    a.getPattern(), b.getPattern(), locations.get());
        }
        return StringSchema.builder()
                .minLength(minLength)
                .maxLength(maxLength)
                .pattern(coalesce(a.getPattern(), b.getPattern()))
                .rawFormat(coalesce(a.getRawFormat(), b.getRawFormat()))
                .build();
    }

    private static NumericSchema mergeNumericSchemas(NumericSchema a, NumericSchema b, Supplier<String> locations) {
        var minimum = maxNullable(a.getMinimum(), b.getMinimum());
        var maximum = minNullable(a.getMaximum(), b.getMaximum());
        var exclusiveMinimum = maxNullable(a.getExclusiveMinimum(), b.getExclusiveMinimum());
        var exclusiveMaximum = minNullable(a.getExclusiveMaximum(), b.getExclusiveMaximum());

        if (minimum != null && maximum != null && minimum.compareTo(maximum) > 0) {
            throw new UnsatisfiableSchemaException(
                    "Numeric range is empty: minimum " + minimum + " exceeds maximum " + maximum + locations.get());
        }
        if (exclusiveMinimum != null && maximum != null && exclusiveMinimum.compareTo(maximum) >= 0) {
            throw new UnsatisfiableSchemaException(
                    "Numeric range is empty: exclusiveMinimum " + exclusiveMinimum
                            + " exceeds maximum " + maximum + locations.get());
        }
        if (minimum != null && exclusiveMaximum != null && minimum.compareTo(exclusiveMaximum) >= 0) {
            throw new UnsatisfiableSchemaException(
                    "Numeric range is empty: minimum " + minimum
                            + " exceeds exclusiveMaximum " + exclusiveMaximum + locations.get());
        }

        var type = a.isInteger() || b.isInteger() ? "integer" : "number";
        return NumericSchema.builder()
                .type(type)
                .minimum(minimum)
                .maximum(maximum)
                .exclusiveMinimum(exclusiveMinimum)
                .exclusiveMaximum(exclusiveMaximum)
                .multipleOf(MathUtil.lcmNullable(a.getMultipleOf(), b.getMultipleOf()))
                .build();
    }

    private static ObjectSchema mergeObjectSchemas(ObjectSchema a, ObjectSchema b, Supplier<String> locations) {
        var properties = new LinkedHashMap<>(a.getProperties());
        for (var entry : b.getProperties().entrySet()) {
            properties.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(left, right, locations));
        }
        var patternProperties = new LinkedHashMap<>(a.getPatternProperties());
        for (var entry : b.getPatternProperties().entrySet()) {
            patternProperties.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(left, right, locations));
        }
        var required = Stream.concat(a.getRequired().stream(), b.getRequired().stream())
                .distinct()
                .toList();
        var additionalProperties = mergeBooleanOrSchema(a.getAdditionalProperties(), b.getAdditionalProperties(), locations);
        return ObjectSchema.builder()
                .properties(properties)
                .patternProperties(patternProperties)
                .required(required)
                .additionalProperties(additionalProperties)
                .minProperties(maxNullable(a.getMinProperties(), b.getMinProperties()))
                .maxProperties(minNullable(a.getMaxProperties(), b.getMaxProperties()))
                .dependentRequired(mergeDependentRequired(a.getDependentRequired(), b.getDependentRequired()))
                .dependentSchemas(mergeDependentSchemas(a.getDependentSchemas(), b.getDependentSchemas(), locations))
                .build();
    }

    /**
     * Merges dependent-required maps by unioning the required-property
     * lists for each trigger key.
     */
    private static Map<String, List<String>> mergeDependentRequired(
            Map<String, List<String>> a, Map<String, List<String>> b) {
        var result = new LinkedHashMap<>(a);
        for (var entry : b.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(),
                    (x, y) -> Stream.concat(x.stream(), y.stream()).distinct().toList());
        }
        return result;
    }

    /**
     * Merges dependent-schemas maps by recursively merging the schema
     * for each shared trigger key.
     */
    private static Map<String, Schema> mergeDependentSchemas(
            Map<String, Schema> a, Map<String, Schema> b, Supplier<String> locations) {
        var result = new LinkedHashMap<>(a);
        for (var entry : b.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(left, right, locations));
        }
        return result;
    }

    private static ArraySchema mergeArraySchemas(ArraySchema a, ArraySchema b, Supplier<String> locations) {
        var items = mergeTwoSchemas(a.getItemSchema(), b.getItemSchema(), locations);
        var contains = mergeContainsClauses(a.getContains(), b.getContains(), locations);
        var prefixA = a.getPrefixSchemas();
        var prefixB = b.getPrefixSchemas();
        List<Schema> mergedPrefix = null;
        if (!prefixA.isEmpty() || !prefixB.isEmpty()) {
            int len = Math.max(prefixA.size(), prefixB.size());
            mergedPrefix = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                var pa = i < prefixA.size() ? prefixA.get(i) : null;
                var pb = i < prefixB.size() ? prefixB.get(i) : null;
                mergedPrefix.add(mergeTwoSchemas(pa, pb, locations));
            }
        }
        var mergedAdditionalItems = a.areAdditionalItemsAllowed() && b.areAdditionalItemsAllowed() ? null : Boolean.FALSE;
        return ArraySchema.builder()
                .items(items)
                .prefixItems(mergedPrefix)
                .additionalItems(mergedAdditionalItems)
                .contains(contains)
                .minItems(maxNullable(a.getMinItems(), b.getMinItems()))
                .maxItems(minNullable(a.getMaxItems(), b.getMaxItems()))
                .uniqueItems(a.isUniqueItems() || b.isUniqueItems())
                .build();
    }

    /**
     * Combines the {@code contains} clauses of two array schemas, or returns
     * {@code null} when neither side states one. Clauses that one and the same
     * element could satisfy are combined into a single clause; clauses that no
     * element could satisfy at once are kept side by side, because each of them
     * only needs some element of the array to satisfy it, not the same one.
     *
     * <p>The clauses returned cover the inputs correctly but are not
     * necessarily the fewest that would.
     */
    private static List<Schema> mergeContainsClauses(List<Schema> a, List<Schema> b, Supplier<String> locations) {
        var allClauses = concat(a, b);
        if (allClauses == null) {
            return null;
        }
        var result = new ArrayList<Schema>();
        // Greedy first-fit: with three or more clauses, which ones end up combined depends on the
        // order they arrive in, so clauses that fewer elements could cover may still yield one
        // clause per element.
        for (var clause : allClauses) {
            boolean combined = false;
            for (int i = 0; i < result.size(); i++) {
                try {
                    var kept = result.get(i);
                    var combinedClause = mergeTwoSchemas(kept, clause, locations);
                    result.set(i, combinedClause);
                    combined = true;
                    break;
                } catch (UnsatisfiableSchemaException incompatible) {
                    // No element can satisfy both — leave that clause alone and try the next.
                    log.trace("keeping contains clause {} separate{}: no single element can satisfy it together "
                            + "with the clause already there: {}", i, locations.get(), incompatible.getMessage());
                }
            }
            if (!combined) {
                result.add(clause);
            }
        }
        return result;
    }

    /**
     * Merges two values that are either {@link Boolean} or {@link Schema}.
     * {@code false} wins over everything; a {@link Schema} wins over
     * {@code true} (more restrictive); two schemas are merged with
     * {@link #mergeTwoSchemas}.
     */
    private static Object mergeBooleanOrSchema(Object a, Object b, Supplier<String> locations) {
        if (Boolean.FALSE.equals(a) || Boolean.FALSE.equals(b)) {
            return Boolean.FALSE;
        }
        if (a instanceof Schema sa && b instanceof Schema sb) {
            return mergeTwoSchemas(sa, sb, locations);
        }
        if (a instanceof Schema) {
            return a;
        }
        if (b instanceof Schema) {
            return b;
        }
        return coalesce(a, b);
    }

    /**
     * Merges two {@code const} values. If both are present they must be
     * equal; otherwise the schemas are unsatisfiable. A single non-null
     * value passes through unchanged.
     */
    private static Object mergeConstValues(Object a, Object b, Supplier<String> locations) {
        if (a == null || b == null) {
            return coalesce(a, b);
        }
        if (!a.equals(b)) {
            throw new UnsatisfiableSchemaException(
                    "Conflicting const values: " + a + " vs " + b + locations.get());
        }
        return a;
    }

    /**
     * Merges two {@code enum} value lists by intersection. If both are
     * present the result contains only values common to both; an empty
     * intersection means the schemas are unsatisfiable. A single non-null
     * list passes through unchanged.
     */
    private static List<Object> mergeEnumValues(List<Object> a, List<Object> b, Supplier<String> locations) {
        if (a == null || b == null) {
            return coalesce(a, b);
        }
        var intersection = a.stream().filter(b::contains).toList();
        if (intersection.isEmpty()) {
            throw new UnsatisfiableSchemaException(
                    "Enum values have no overlap: " + a + " vs " + b + locations.get());
        }
        return intersection;
    }

    /**
     * Returns the JSON Schema type name for a schema — e.g. {@code "string"},
     * {@code "integer"}, {@code "object"}. For numeric schemas the distinction
     * between integer and number is preserved; for all others the class name
     * is lowercased with the {@code Schema} suffix stripped.
     */
    private static String typeName(Schema schema) {
        if (schema instanceof NumericSchema ns) {
            return ns.isInteger() ? "integer" : "number";
        }
        return schema.getClass().getSimpleName().replace("Schema", "").toLowerCase();
    }
}
