package io.github.gjuton.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public final class StringSchema extends Schema {

    private Integer minLength;
    private Integer maxLength;
    private String pattern;

    /**
     * The declared {@code format} exactly as the schema names it, or
     * {@code null} when it names none. Formats gjuton has no generator for
     * are kept alongside the ones it models.
     */
    @JsonProperty("format")
    private String rawFormat;

    /**
     * The declared format as a modelled constant, or {@code null} when the
     * schema names no format or one gjuton does not model. Consult
     * {@link #getRawFormat()} to tell those two apart.
     */
    @JsonIgnore
    public StringFormat getFormat() {
        return StringFormat.fromValue(rawFormat);
    }
}
