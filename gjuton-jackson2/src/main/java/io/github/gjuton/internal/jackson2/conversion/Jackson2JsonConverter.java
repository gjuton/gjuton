package io.github.gjuton.internal.jackson2.conversion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gjuton.errors.JsonBindingException;
import io.github.gjuton.internal.jsonconversion.JsonConverter;

/**
 * The {@link JsonConverter} backed by Jackson 2 databind.
 *
 * <p>Converts whatever the mapper it was given is configured to convert;
 * which types need special handling is not its concern.
 */
public final class Jackson2JsonConverter implements JsonConverter {

    private final ObjectMapper mapper;

    public Jackson2JsonConverter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * A mapper builder settled on the answers {@link JsonConverter}
     * promises, leaving the types being converted to the caller. Every
     * setting the promises depend on is stated rather than inherited, so a
     * Jackson version whose defaults differ converts the same way.
     */
    public static JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, false)
                .configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
                .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
                .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false)
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false);
    }

    @Override
    public Object readTree(String json) {
        try {
            return mapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new JsonBindingException("Cannot read JSON", e);
        }
    }

    @Override
    public <T> T convert(Object tree, Class<T> type) {
        try {
            return mapper.convertValue(tree, type);
        } catch (IllegalArgumentException e) {
            throw new JsonBindingException("Value does not map onto " + type.getName(), e);
        }
    }

    @Override
    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new JsonBindingException("Cannot write value as JSON", e);
        }
    }
}
