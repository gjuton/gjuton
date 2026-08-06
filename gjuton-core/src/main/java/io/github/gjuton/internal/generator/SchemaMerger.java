package io.github.gjuton.internal.generator;

import static io.github.gjuton.internal.util.CollectionUtil.concat;
import static io.github.gjuton.internal.util.FunctionalUtil.coalesce;
import static io.github.gjuton.internal.util.MathUtil.maxNullable;
import static io.github.gjuton.internal.util.MathUtil.minNullable;

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
import io.github.gjuton.internal.util.MathUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
     * For tests only, to save passing a context.
     */
    static Schema merge(List<Schema> schemas) {
        return merge(schemas, null, null);
    }

    /**
     * Merges a list of schemas pairwise from left to right, producing a single
     * schema whose constraints are the intersection of all inputs. A schema
     * standing for a {@code $ref} is merged as the definition it names.
     *
     * @throws IllegalArgumentException      if the list is empty
     * @throws UnsatisfiableSchemaException   if any pair has incompatible types or constraints
     */
    static Schema merge(GeneratorContext context, List<Schema> schemas) {
        return merge(context, schemas, null, null);
    }

    /**
     * Like {@link #merge(GeneratorContext, List)}, but conflict exceptions are
     * enriched with the given {@code locationList} and {@code schemaPath}.
     */
    static Schema merge(GeneratorContext context, List<Schema> schemas, List<String> locationList, String schemaPath) {
        if (schemas.isEmpty()) {
            throw new IllegalArgumentException("merge requires at least one schema");
        }
        var locations = buildLocations(locationList, schemaPath);
        var result = schemas.get(0);
        for (int i = 1; i < schemas.size(); i++) {
            result = mergeTwoSchemas(context, result, schemas.get(i), locations);
        }
        return result;
    }

    /**
     * For tests only, to save passing a context. Conflict exceptions are
     * enriched with the given {@code locationList} and {@code schemaPath}.
     *
     * @param locationList JSON Pointers identifying each schema's origin
     *                   (e.g. {@code /allOf/0}), or {@code null} to omit
     * @param schemaPath position in the generated document
     *                   (e.g. {@code /address}), or {@code null} to omit
     */
    static Schema merge(List<Schema> schemas, List<String> locationList, String schemaPath) {
        return merge(null, schemas, locationList, schemaPath);
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
     * only the compatible results. A schema standing for a {@code $ref} is
     * merged as the definition it names. Schemas that are incompatible with
     * {@code other} are omitted from the returned list without failing the
     * merge, so the returned list may be shorter than {@code schemas} and
     * nothing else signals that to the caller.
     */
    static List<Schema> mergeEachWith(GeneratorContext context, List<Schema> schemas, Schema other) {
        var result = new ArrayList<Schema>();
        for (int i = 0; i < schemas.size(); i++) {
            try {
                result.add(merge(context, List.of(schemas.get(i), other), null, null));
            } catch (UnsatisfiableSchemaException incompatible) {
                log.trace("dropping branch {}: incompatible with the parent schema: {}", i, incompatible.getMessage());
            }
        }
        return result;
    }

    /**
     * The definition a schema standing for a {@code $ref} names, or the schema
     * as written when it names none or there is no context to resolve against.
     */
    private static Schema resolveRef(GeneratorContext context, Schema schema) {
        if (context == null || schema.getRef() == null) {
            return schema;
        }
        return context.resolveRef(schema.getRef());
    }

    private static Schema mergeTwoSchemas(GeneratorContext context, Schema a, Schema b, Supplier<String> locations) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        var resolvedA = resolveRef(context, a);
        if (resolvedA != a) {
            return mergeTwoSchemas(context, resolvedA, b, locations);
        }
        var resolvedB = resolveRef(context, b);
        if (resolvedB != b) {
            return mergeTwoSchemas(context, a, resolvedB, locations);
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
        } else if (a instanceof BooleanSchema && b instanceof BooleanSchema) {
            // Booleans carry no constraints of their own, so either side stands
            // for both.
            merged = a.toBuilder().build();
        } else if (a instanceof NullSchema && b instanceof NullSchema) {
            // Null carries no constraints of its own, so either side stands
            // for both.
            merged = a.toBuilder().build();
        } else if (a instanceof StringSchema sa && b instanceof StringSchema sb) {
            merged = mergeStringSchemas(sa, sb, locations);
        } else if (a instanceof NumericSchema na && b instanceof NumericSchema nb) {
            merged = mergeNumericSchemas(na, nb, locations);
        } else if (a instanceof ObjectSchema oa && b instanceof ObjectSchema ob) {
            merged = mergeObjectSchemas(context, oa, ob, locations);
        } else if (a instanceof ArraySchema aa && b instanceof ArraySchema ab) {
            merged = mergeArraySchemas(context, aa, ab, locations);
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
                .oneOf(unionGroups(a.getOneOf(), b.getOneOf()))
                .anyOf(unionGroups(a.getAnyOf(), b.getAnyOf()))
                .allOf(concat(a.getAllOf(), b.getAllOf()));
        mergeConditional(builder, a, b);
        return builder.build();
    }

    /**
     * Combines the {@code anyOf} or {@code oneOf} groups of both sides, keeping
     * one copy of a group both carry — each has to hold on its own, and a group
     * is a choice among its branches, so branch order tells none apart. Returns
     * {@code null} when neither side has any.
     */
    private static List<List<Schema>> unionGroups(List<List<Schema>> a, List<List<Schema>> b) {
        var combined = concat(a, b);
        if (combined == null) {
            return null;
        }
        var seen = new HashSet<Set<Schema>>();
        var distinct = new ArrayList<List<Schema>>();
        for (var group : combined) {
            if (seen.add(Set.copyOf(group))) {
                distinct.add(group);
            }
        }
        return List.copyOf(distinct);
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

    private static ObjectSchema mergeObjectSchemas(GeneratorContext context,
            ObjectSchema a, ObjectSchema b, Supplier<String> locations) {
        rejectRequiredBannedBy(a, b, locations);
        rejectRequiredBannedBy(b, a, locations);
        var properties = new LinkedHashMap<>(a.getProperties());
        for (var entry : b.getProperties().entrySet()) {
            properties.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(context, left, right, locations));
        }
        var patternProperties = new LinkedHashMap<>(a.getPatternProperties());
        for (var entry : b.getPatternProperties().entrySet()) {
            patternProperties.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(context, left, right, locations));
        }
        var required = Stream.concat(a.getRequired().stream(), b.getRequired().stream())
                .distinct()
                .toList();
        var additionalProperties = mergeBooleanOrSchema(context, a.getAdditionalProperties(), b.getAdditionalProperties(), locations);
        return ObjectSchema.builder()
                .properties(properties)
                .patternProperties(patternProperties)
                .required(required)
                .additionalProperties(additionalProperties)
                .minProperties(maxNullable(a.getMinProperties(), b.getMinProperties()))
                .maxProperties(minNullable(a.getMaxProperties(), b.getMaxProperties()))
                .dependentRequired(mergeDependentRequired(a.getDependentRequired(), b.getDependentRequired()))
                .dependentSchemas(mergeDependentSchemas(context, a.getDependentSchemas(), b.getDependentSchemas(), locations))
                .build();
    }

    /**
     * Rejects a merge in which {@code demanding} requires a property
     * {@code closed} does not allow — no value can carry it and satisfy both.
     *
     * @throws UnsatisfiableSchemaException if such a property exists
     */
    private static void rejectRequiredBannedBy(ObjectSchema demanding, ObjectSchema closed, Supplier<String> locations) {
        if (!Boolean.FALSE.equals(closed.getAdditionalProperties())) {
            return;
        }
        for (var name : demanding.getRequired()) {
            if (closed.getProperties().containsKey(name)) {
                continue;
            }
            var patterns = closed.getPatternProperties().keySet();
            var allowedByPattern = patterns.stream()
                    .anyMatch(pattern -> Pattern.compile(pattern).matcher(name).find());
            if (allowedByPattern) {
                continue;
            }
            throw new UnsatisfiableSchemaException(
                    "required property '" + name + "' is not allowed by additionalProperties" + locations.get());
        }
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
    private static Map<String, Schema> mergeDependentSchemas(GeneratorContext context,
            Map<String, Schema> a, Map<String, Schema> b, Supplier<String> locations) {
        var result = new LinkedHashMap<>(a);
        for (var entry : b.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(),
                    (left, right) -> mergeTwoSchemas(context, left, right, locations));
        }
        return result;
    }

    private static ArraySchema mergeArraySchemas(GeneratorContext context,
            ArraySchema a, ArraySchema b, Supplier<String> locations) {
        var items = mergeTwoSchemas(context, a.getItemSchema(), b.getItemSchema(), locations);
        var contains = mergeContainsClauses(context, a.getContains(), b.getContains(), locations);
        var prefixA = a.getPrefixSchemas();
        var prefixB = b.getPrefixSchemas();
        List<Schema> mergedPrefix = null;
        if (!prefixA.isEmpty() || !prefixB.isEmpty()) {
            int len = Math.max(prefixA.size(), prefixB.size());
            mergedPrefix = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                var pa = i < prefixA.size() ? prefixA.get(i) : null;
                var pb = i < prefixB.size() ? prefixB.get(i) : null;
                mergedPrefix.add(mergeTwoSchemas(context, pa, pb, locations));
            }
        }
        var mergedAdditionalItems = a.areAdditionalItemsAllowed() && b.areAdditionalItemsAllowed() ? null : Boolean.FALSE;
        var minItems = maxNullable(a.getMinItems(), b.getMinItems());
        var maxItems = minNullable(a.getMaxItems(), b.getMaxItems());
        if (minItems != null && maxItems != null && minItems > maxItems) {
            throw new UnsatisfiableSchemaException(
                    "Array length range is empty: minItems " + minItems
                            + " exceeds maxItems " + maxItems + locations.get());
        }
        return ArraySchema.builder()
                .items(items)
                .prefixItems(mergedPrefix)
                .additionalItems(mergedAdditionalItems)
                .contains(contains)
                .minItems(minItems)
                .maxItems(maxItems)
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
    private static List<Schema> mergeContainsClauses(GeneratorContext context,
            List<Schema> a, List<Schema> b, Supplier<String> locations) {
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
                    var combinedClause = mergeTwoSchemas(context, kept, clause, locations);
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
    private static Object mergeBooleanOrSchema(GeneratorContext context,
            Object a, Object b, Supplier<String> locations) {
        if (Boolean.FALSE.equals(a) || Boolean.FALSE.equals(b)) {
            return Boolean.FALSE;
        }
        if (a instanceof Schema sa && b instanceof Schema sb) {
            return mergeTwoSchemas(context, sa, sb, locations);
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
