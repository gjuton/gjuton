package io.github.gjuton.internal.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A schema that is a {@code $ref} reference to another schema. Per
 * Draft-07 §8.3, a {@code $ref} replaces the entire schema — all sibling
 * keywords are ignored.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
public final class RefSchema extends Schema {

    private String ref;
}
