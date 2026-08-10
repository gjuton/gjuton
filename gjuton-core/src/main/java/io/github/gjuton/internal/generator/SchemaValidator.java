package io.github.gjuton.internal.generator;

import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.model.BooleanSchema;
import io.github.gjuton.internal.model.NullSchema;
import io.github.gjuton.internal.model.NumericSchema;
import io.github.gjuton.internal.model.ObjectSchema;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.model.StringFormat;
import io.github.gjuton.internal.model.StringSchema;
import io.github.gjuton.internal.model.UnsatisfiableSchema;
import io.github.gjuton.internal.model.UntypedSchema;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Checks whether a generated value actually satisfies a schema, independent
 * of how the value was produced.
 *
 * <p>{@link SchemaMerger} approximates constraints it cannot express by
 * intersecting schemas (a kept-left {@code pattern} on conflict, a chosen
 * {@code oneOf} branch that turns out to also match another branch). This
 * validator re-checks a candidate value against the original, unmerged
 * schema so the generator can detect and retry a candidate that the
 * approximation let through incorrectly.
 */
final class SchemaValidator {

    private final GeneratorContext context;

    SchemaValidator(GeneratorContext context) {
        this.context = context;
    }

    /**
     * Recursion ceiling for {@code $ref} chains. A cyclic schema (a self- or
     * mutually-referencing definition with no other constraints) would
     * otherwise resolve forever without ever consuming any of the value's
     * structure. Past this depth we can no longer prove a violation, so we
     * conservatively treat the value as satisfying the schema.
     */
    private static final int MAX_REF_DEPTH = 100;

    /**
     * Ceiling on how much of a value a violation message quotes, so one
     * oversized string or object cannot push the rest of the line off screen.
     */
    private static final int MAX_DESCRIBED_LENGTH = 60;

    /**
     * Ceiling on how many branches of an unmatched {@code oneOf}/{@code anyOf}
     * group explain themselves, so a group of dozens stays readable.
     */
    private static final int MAX_REPORTED_BRANCHES = 3;

    boolean satisfies(Object value, Schema schema) {
        return violation(value, schema) == null;
    }

    private boolean satisfies(Object value, Schema schema, int refDepth) {
        return violation(value, schema, refDepth) == null;
    }

    /**
     * Names the first constraint {@code value} violates, or {@code null} if it
     * satisfies {@code schema}. A nested violation reads outermost-first, so
     * the message locates the offending part of the value as well as naming
     * the constraint. Long values are abbreviated.
     */
    String violation(Object value, Schema schema) {
        return violation(value, schema, 0);
    }

    private String violation(Object value, Schema schema, int refDepth) {
        if (value instanceof OverriddenValue) {
            // A caller-supplied override is exempt from validation; the caller
            // owns its correctness.
            return null;
        }
        if (schema.getRef() != null) {
            if (refDepth >= MAX_REF_DEPTH) {
                return null;
            }
            return violation(value, context.resolveRef(schema.getRef()), refDepth + 1);
        }
        if (schema.getConstValue() != null && !valuesEqual(schema.getConstValue(), value)) {
            return "const mismatch: expected " + describe(schema.getConstValue()) + ", got " + describe(value);
        }
        if (schema.getEnumValues() != null
                && schema.getEnumValues().stream().noneMatch(allowed -> valuesEqual(allowed, value))) {
            return describe(value) + " is not one of the " + schema.getEnumValues().size() + " enum values";
        }
        if (schema.getAllOf() != null) {
            for (int i = 0; i < schema.getAllOf().size(); i++) {
                var nested = violation(value, schema.getAllOf().get(i), refDepth);
                if (nested != null) {
                    return "allOf branch " + i + ": " + nested;
                }
            }
        }
        if (schema.getAnyOf() != null) {
            for (var group : schema.getAnyOf()) {
                if (group.stream().noneMatch(branch -> satisfies(value, branch, refDepth))) {
                    return "no anyOf branch matched — " + branchViolations(value, group, refDepth);
                }
            }
        }
        if (schema.getOneOf() != null) {
            for (var group : schema.getOneOf()) {
                long matched = group.stream().filter(branch -> satisfies(value, branch, refDepth)).count();
                if (matched == 0) {
                    return "no oneOf branch matched — " + branchViolations(value, group, refDepth);
                }
                if (matched > 1) {
                    return matched + " of " + group.size() + " oneOf branches matched, expected exactly 1";
                }
            }
        }
        for (var conditional : schema.getConditionals()) {
            boolean ifMatched = satisfies(value, conditional.ifSchema(), refDepth);
            var branch = ifMatched ? conditional.thenSchema() : conditional.elseSchema();
            if (branch != null) {
                var nested = violation(value, branch, refDepth);
                if (nested != null) {
                    return (ifMatched ? "then" : "else") + " branch: " + nested;
                }
            }
        }
        if (schema.getNotSchema() != null && satisfies(value, schema.getNotSchema(), refDepth)) {
            return describe(value) + " matches the not schema";
        }
        return switch (schema) {
            case StringSchema s -> stringViolation(value, s);
            case NumericSchema s -> numericViolation(value, s);
            case BooleanSchema ignored -> value instanceof Boolean ? null : typeMismatch("boolean", value);
            case NullSchema ignored -> value == null ? null : typeMismatch("null", value);
            case ObjectSchema s -> objectViolation(value, s);
            case ArraySchema s -> arrayViolation(value, s);
            case UntypedSchema ignored -> null;
            case UnsatisfiableSchema ignored -> "the schema admits no value at all";
        };
    }

    /**
     * {@code format} is intentionally not checked: it never causes the
     * zero-/multiple-branch failures this validator exists to catch, and
     * validating it properly means re-implementing an email/URI/IPv6/etc.
     * validator per {@link StringFormat}
     * variant.
     */
    private String stringViolation(Object value, StringSchema schema) {
        if (!(value instanceof String s)) {
            return typeMismatch("string", value);
        }
        if (schema.getMinLength() != null && s.length() < schema.getMinLength()) {
            return "length " + s.length() + " is below minLength " + schema.getMinLength();
        }
        if (schema.getMaxLength() != null && s.length() > schema.getMaxLength()) {
            return "length " + s.length() + " is above maxLength " + schema.getMaxLength();
        }
        if (schema.getPattern() != null && !Pattern.compile(schema.getPattern()).matcher(s).find()) {
            return describe(s) + " does not match pattern " + schema.getPattern();
        }
        return null;
    }

    private String numericViolation(Object value, NumericSchema schema) {
        if (!(value instanceof Number n)) {
            return typeMismatch("number", value);
        }
        var v = toBigDecimal(n);
        if (schema.isInteger() && v.stripTrailingZeros().scale() > 0) {
            return v + " is not an integer";
        }
        if (schema.getMinimum() != null && v.compareTo(schema.getMinimum()) < 0) {
            return v + " is below minimum " + schema.getMinimum();
        }
        if (schema.getMaximum() != null && v.compareTo(schema.getMaximum()) > 0) {
            return v + " is above maximum " + schema.getMaximum();
        }
        if (schema.getExclusiveMinimum() != null && v.compareTo(schema.getExclusiveMinimum()) <= 0) {
            return v + " is not above exclusiveMinimum " + schema.getExclusiveMinimum();
        }
        if (schema.getExclusiveMaximum() != null && v.compareTo(schema.getExclusiveMaximum()) >= 0) {
            return v + " is not below exclusiveMaximum " + schema.getExclusiveMaximum();
        }
        if (schema.getMultipleOf() != null
                && v.remainder(schema.getMultipleOf()).compareTo(BigDecimal.ZERO) != 0) {
            return v + " is not a multiple of " + schema.getMultipleOf();
        }
        return null;
    }

    private String objectViolation(Object value, ObjectSchema schema) {
        if (!(value instanceof Map<?, ?> map)) {
            return typeMismatch("object", value);
        }
        for (var required : schema.getRequired()) {
            if (!map.containsKey(required)) {
                return "missing required property '" + required + "'";
            }
        }
        if (schema.getMinProperties() != null && map.size() < schema.getMinProperties()) {
            return map.size() + " properties is below minProperties " + schema.getMinProperties();
        }
        if (schema.getMaxProperties() != null && map.size() > schema.getMaxProperties()) {
            return map.size() + " properties is above maxProperties " + schema.getMaxProperties();
        }
        for (var entry : map.entrySet()) {
            if (schema.getPropertyNames() != null) {
                var nested = violation(entry.getKey(), schema.getPropertyNames());
                if (nested != null) {
                    return "property name '" + entry.getKey() + "': " + nested;
                }
            }
            var propertySchema = schema.getProperties().get(entry.getKey());
            if (propertySchema != null) {
                var nested = violation(entry.getValue(), propertySchema);
                if (nested != null) {
                    return "property '" + entry.getKey() + "': " + nested;
                }
            } else if (Boolean.FALSE.equals(schema.getAdditionalProperties())) {
                return "property '" + entry.getKey() + "' is not allowed by additionalProperties";
            } else if (schema.getAdditionalProperties() instanceof Schema additionalSchema) {
                var nested = violation(entry.getValue(), additionalSchema);
                if (nested != null) {
                    return "additional property '" + entry.getKey() + "': " + nested;
                }
            }
        }
        for (var entry : schema.getDependentRequired().entrySet()) {
            if (map.containsKey(entry.getKey()) && !map.keySet().containsAll(entry.getValue())) {
                return "property '" + entry.getKey() + "' requires " + entry.getValue();
            }
        }
        for (var entry : schema.getDependentSchemas().entrySet()) {
            if (map.containsKey(entry.getKey())) {
                var nested = violation(value, entry.getValue());
                if (nested != null) {
                    return "dependentSchemas for '" + entry.getKey() + "': " + nested;
                }
            }
        }
        return null;
    }

    private String arrayViolation(Object value, ArraySchema schema) {
        if (!(value instanceof List<?> list)) {
            return typeMismatch("array", value);
        }
        if (schema.getMinItems() != null && list.size() < schema.getMinItems()) {
            return list.size() + " items is below minItems " + schema.getMinItems();
        }
        if (schema.getMaxItems() != null && list.size() > schema.getMaxItems()) {
            return list.size() + " items is above maxItems " + schema.getMaxItems();
        }
        var prefixSchemas = schema.getPrefixSchemas();
        var itemSchema = schema.getItemSchema();
        for (int i = 0; i < list.size(); i++) {
            if (i < prefixSchemas.size()) {
                var nested = violation(list.get(i), prefixSchemas.get(i));
                if (nested != null) {
                    return "item " + i + ": " + nested;
                }
            } else if (!schema.areAdditionalItemsAllowed()) {
                return "item " + i + " is beyond the " + prefixSchemas.size() + " allowed prefix items";
            } else if (itemSchema != null) {
                var nested = violation(list.get(i), itemSchema);
                if (nested != null) {
                    return "item " + i + ": " + nested;
                }
            }
        }
        if (schema.getContains() != null) {
            for (var contains : schema.getContains()) {
                if (list.stream().noneMatch(item -> satisfies(item, contains))) {
                    return "no item satisfies the contains schema";
                }
            }
        }
        if (schema.isUniqueItems() && new HashSet<>(list).size() != list.size()) {
            return "items are not unique";
        }
        return null;
    }

    /**
     * Why each branch of a {@code oneOf}/{@code anyOf} group rejected the
     * value, so a group that nothing matched explains itself rather than only
     * reporting the count. Long groups are cut short, with the number of
     * branches left unreported stated rather than silently dropped.
     */
    private String branchViolations(Object value, List<Schema> group, int refDepth) {
        int reported = Math.min(group.size(), MAX_REPORTED_BRANCHES);
        var reasons = new StringBuilder();
        for (int i = 0; i < reported; i++) {
            if (i > 0) {
                reasons.append("; ");
            }
            reasons.append("branch ").append(i).append(": ").append(violation(value, group.get(i), refDepth));
        }
        if (group.size() > reported) {
            reasons.append("; and ").append(group.size() - reported).append(" more");
        }
        return reasons.toString();
    }

    private static String typeMismatch(String expected, Object value) {
        if (value == null) {
            return "expected " + expected + ", got null";
        }
        return "expected " + expected + ", got " + value.getClass().getSimpleName();
    }

    /**
     * A value rendered short enough to sit inside a log line, abbreviated
     * with a trailing ellipsis when it would otherwise run long.
     */
    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        var rendered = value instanceof String s ? "'" + s + "'" : value.toString();
        if (rendered.length() <= MAX_DESCRIBED_LENGTH) {
            return rendered;
        }
        return rendered.substring(0, MAX_DESCRIBED_LENGTH) + "...";
    }

    /**
     * Equality between JSON values as JSON Schema means it: two numbers are
     * equal when numerically equal regardless of their Java representation
     * (an {@code integer} 1 and a {@code number} 1.0 are equal), and all
     * other values compare by {@link Object#equals}.
     */
    private static boolean valuesEqual(Object a, Object b) {
        if (a instanceof Number an && b instanceof Number bn) {
            return toBigDecimal(an).compareTo(toBigDecimal(bn)) == 0;
        }
        return Objects.equals(a, b);
    }

    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal bd) {
            return bd;
        }
        if (n instanceof Double || n instanceof Float) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return BigDecimal.valueOf(n.longValue());
    }
}
