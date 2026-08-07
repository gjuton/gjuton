package io.github.gjuton.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Schema for both {@code "type": "integer"} and {@code "type": "number"}.
 * The {@link #type} field distinguishes the two: integer schemas produce
 * whole-number values, number schemas produce fractional values.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumericSchema extends Schema {

    private String type;

    private BigDecimal minimum;
    private BigDecimal maximum;

    /**
     * The JSON Schema {@code exclusiveMinimum} keyword. Can be:
     * <ul>
     *     <li>A {@link BigDecimal} — the exclusive bound itself (Draft 7)</li>
     *     <li>A {@link Boolean} — whether {@link #minimum} is exclusive (Draft 4)</li>
     * </ul>
     */
    @Getter(lombok.AccessLevel.NONE)
    private Object exclusiveMinimum;

    /**
     * The JSON Schema {@code exclusiveMaximum} keyword. Can be:
     * <ul>
     *     <li>A {@link BigDecimal} — the exclusive bound itself (Draft 7)</li>
     *     <li>A {@link Boolean} — whether {@link #maximum} is exclusive (Draft 4)</li>
     * </ul>
     */
    @Getter(lombok.AccessLevel.NONE)
    private Object exclusiveMaximum;

    private BigDecimal multipleOf;

    /**
     * Returns the value generated numbers must exceed, or {@code null} when
     * the schema states no exclusive lower bound. Draft 4 carries the bound
     * in {@link #minimum} and only marks it exclusive; Draft 7 carries it in
     * the keyword itself. Both resolve to the bound.
     */
    public BigDecimal getExclusiveMinimum() {
        if (exclusiveMinimum instanceof BigDecimal bound) {
            return bound;
        }
        return Boolean.TRUE.equals(exclusiveMinimum) ? minimum : null;
    }

    /**
     * Returns the value generated numbers must stay below, or {@code null}
     * when the schema states no exclusive upper bound. Draft 4 carries the
     * bound in {@link #maximum} and only marks it exclusive; Draft 7 carries
     * it in the keyword itself. Both resolve to the bound.
     */
    public BigDecimal getExclusiveMaximum() {
        if (exclusiveMaximum instanceof BigDecimal bound) {
            return bound;
        }
        return Boolean.TRUE.equals(exclusiveMaximum) ? maximum : null;
    }

    /**
     * Whether this schema requires integer values.
     */
    public boolean isInteger() {
        return "integer".equals(type);
    }
}
