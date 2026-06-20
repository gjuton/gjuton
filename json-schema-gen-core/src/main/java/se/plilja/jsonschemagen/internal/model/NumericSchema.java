package se.plilja.jsonschemagen.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class NumericSchema extends Schema {

    private Long minimum;
    private Long maximum;
    private Long exclusiveMinimum;
    private Long exclusiveMaximum;
    private Long multipleOf;
}
