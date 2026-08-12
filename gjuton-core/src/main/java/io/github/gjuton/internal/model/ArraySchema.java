package io.github.gjuton.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Schema for JSON arrays. Handles both uniform arrays ({@code items} as
 * a single schema) and tuple-typed arrays ({@code items} as an array of
 * positional schemas in Draft 7, or {@code prefixItems} in Draft 2019-09+).
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ArraySchema extends Schema {

    /**
     * The JSON Schema {@code items} keyword. Can be:
     * <ul>
     *     <li>A {@link Schema} — uniform constraint on every element</li>
     *     <li>A {@code List<Schema>} — positional (tuple) constraint (Draft 7)</li>
     *     <li>An {@link UnsatisfiableSchema} — no elements allowed past {@code prefixItems} (Draft 2019-09+)</li>
     * </ul>
     */
    @Getter(lombok.AccessLevel.NONE)
    private Object items;

    /**
     * Draft 2019-09+ positional schemas. Each entry constrains the
     * element at the corresponding array index.
     */
    private List<Schema> prefixItems;

    /**
     * Constraint on elements beyond the tuple. Either
     * {@link Boolean} ({@code true}/{@code false}) or a {@link Schema}.
     * Corresponds to Draft 7 {@code additionalItems}.
     */
    @Getter(lombok.AccessLevel.NONE)
    private Object additionalItems;

    /**
     * Clauses the array must satisfy, or {@code null} when it carries none.
     * A document states one through {@code contains}; merging schemas that
     * each declare one accumulates them, and every clause then holds on its
     * own.
     */
    private List<Schema> contains;

    @JsonSetter("contains")
    private void setContains(Schema contains) {
        this.contains = contains == null ? null : List.of(contains);
    }

    /**
     * The fewest elements each {@code contains} clause needs. One when
     * absent, ignored without {@code contains}. A single bound covers
     * every clause, so merged branches asking for different counts all end
     * up under the strictest — narrower than they demanded, never wrong.
     */
    private Integer minContains;

    /**
     * The most elements each {@code contains} clause may have. Unlimited
     * when absent, ignored without {@code contains}. As with
     * {@code minContains}, one bound covers every clause.
     */
    private Integer maxContains;

    private Integer minItems;
    private Integer maxItems;

    /**
     * Whether every element of the array must be distinct from every
     * other element. Corresponds to the JSON Schema {@code uniqueItems}
     * keyword; defaults to {@code false} when absent.
     */
    private boolean uniqueItems;

    /**
     * Returns the positional (tuple) schemas, merging both Draft 7
     * ({@code items} as array) and Draft 2019-09+ ({@code prefixItems}).
     * Returns an empty list when neither is present.
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<Schema> getPrefixSchemas() {
        if (prefixItems != null) {
            return List.copyOf(prefixItems);
        }
        if (items instanceof List<?> list) {
            return (List<Schema>) list;
        }
        return List.of();
    }

    /**
     * Returns the effective schema that array elements must satisfy.
     * Checks {@code additionalItems} first (Draft 7 tuple constraint),
     * then falls back to {@code items} as a single schema. Returns
     * {@code null} when no schema constraint is specified.
     */
    @JsonIgnore
    public Schema getItemSchema() {
        if (additionalItems instanceof Schema s) {
            return s;
        }
        if (items instanceof Schema s) {
            return s;
        }
        return null;
    }

    /**
     * Whether elements beyond the prefix items are allowed.
     */
    @JsonIgnore
    public boolean areAdditionalItemsAllowed() {
        if (Boolean.FALSE.equals(additionalItems)) {
            return false;
        }
        if (prefixItems != null && items instanceof UnsatisfiableSchema) {
            return false;
        }
        return true;
    }

}
