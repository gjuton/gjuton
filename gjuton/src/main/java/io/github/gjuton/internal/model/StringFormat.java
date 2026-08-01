package io.github.gjuton.internal.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The values of the JSON Schema {@code format} keyword gjuton models. A schema
 * may name any other format; the generator treats those as plain strings.
 */
public enum StringFormat {
    DATE("date"),
    DATE_TIME("date-time"),
    TIME("time"),
    EMAIL("email"),
    IDN_EMAIL("idn-email"),
    URI("uri"),
    URI_REFERENCE("uri-reference"),
    IRI("iri"),
    IRI_REFERENCE("iri-reference"),
    HOSTNAME("hostname"),
    IDN_HOSTNAME("idn-hostname"),
    IPV4("ipv4"),
    IPV6("ipv6"),
    UUID("uuid"),
    REGEX("regex"),
    JSON_POINTER("json-pointer"),
    RELATIVE_JSON_POINTER("relative-json-pointer"),
    DURATION("duration");

    private final String value;

    StringFormat(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    /**
     * The constant for {@code value}, or {@code null} when it names a format
     * gjuton does not model or no format at all.
     */
    @JsonCreator
    public static StringFormat fromValue(String value) {
        for (var format : values()) {
            if (format.value.equals(value)) {
                return format;
            }
        }
        return null;
    }
}
