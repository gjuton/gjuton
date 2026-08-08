package io.github.gjuton.internal.jsonconversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gjuton.errors.JsonBindingException;
import io.github.gjuton.internal.model.Schema;
import io.github.gjuton.internal.parser.SchemaParser;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the behaviour {@link JsonConverter} promises. It runs against
 * whichever converter is on the classpath, so every implementation is held
 * to the same answers rather than to a copy of this file.
 */
class JsonConverterContractTest {

    private static final JsonConverter CONVERTER = JsonConverters.get();

    record Bean(int a) {
    }

    @Test
    void readTreeBindsAnIntegralNumberToTheNarrowestTypeThatHoldsIt() {
        // when
        var tree = CONVERTER.readTree("""
                {
                    "small": 7,
                    "long": 9223372036854775807,
                    "huge": 9223372036854775808,
                    "fraction": 1.5
                }
                """);

        // then
        var root = (Map<?, ?>) tree;
        assertThat(root.get("small")).isEqualTo(7);
        assertThat(root.get("long")).isEqualTo(Long.MAX_VALUE);
        assertThat(root.get("huge")).isEqualTo(new BigInteger("9223372036854775808"));
        assertThat(root.get("fraction")).isEqualTo(1.5d);
    }

    @Test
    void readTreeBindsNestingToMapsAndLists() {
        // when
        var tree = CONVERTER.readTree("""
                {
                    "type": "array",
                    "items": [
                        {
                            "type": "string"
                        }
                    ]
                }
                """);

        // then
        var root = (Map<?, ?>) tree;
        assertThat(root.get("items")).isInstanceOf(List.class);
        var items = (List<?>) root.get("items");
        assertThat(items.get(0)).isInstanceOf(Map.class);
    }

    @Test
    void convertProducesTheSchemaModelTheParserProduces() {
        // given a schema exercising every combining keyword
        var json = """
                {
                    "allOf": [
                        {
                            "type": "string"
                        }
                    ],
                    "anyOf": [
                        {
                            "type": "integer"
                        }
                    ],
                    "oneOf": [
                        {
                            "type": "boolean"
                        }
                    ],
                    "not": {
                        "type": "null"
                    },
                    "if": {
                        "type": "string"
                    },
                    "then": {
                        "type": "string",
                        "minLength": 2
                    },
                    "else": {
                        "type": "integer"
                    }
                }
                """;

        // when
        var tree = CONVERTER.readTree(json);
        var converted = CONVERTER.convert(tree, Schema.class);

        // then
        var parsed = SchemaParser.parse(json).getRoot();
        assertThat(converted).isEqualTo(parsed);
    }

    @Test
    void writeKeepsTheOrderAndShapeOfTheTree() {
        // given
        var inner = new ArrayList<Object>();
        inner.add(true);
        inner.add(null);
        inner.add("x");
        inner.add(1.5d);
        var tree = new LinkedHashMap<String, Object>();
        tree.put("b", 1);
        tree.put("a", inner);

        // when
        var json = CONVERTER.write(tree);

        // then
        assertThat(json).isEqualTo("{\"b\":1,\"a\":[true,null,\"x\",1.5]}");
    }

    @Test
    void convertOntoATypeTheTreeDoesNotMapOntoFails() {
        // given
        var tree = List.of(1, 2);

        // when / then
        assertThatThrownBy(() -> CONVERTER.convert(tree, Bean.class))
                .isInstanceOf(JsonBindingException.class);
    }

    @Test
    void convertOntoATypeThatDoesNotDeclareThePropertyFails() {
        // given
        var tree = Map.of("a", 1, "unknown", 2);

        // when / then
        assertThatThrownBy(() -> CONVERTER.convert(tree, Bean.class))
                .isInstanceOf(JsonBindingException.class);
    }

    @Test
    void convertIgnoresASchemaKeywordNoModelClassDeclares() {
        // when
        var tree = CONVERTER.readTree("""
                {
                    "type": "string",
                    "x-vendor-extension": {
                        "anything": true
                    }
                }
                """);
        var schema = CONVERTER.convert(tree, Schema.class);

        // then
        assertThat(schema).isNotNull();
    }

    @Test
    void readTreeIgnoresContentAfterTheFirstCompleteValue() {
        // when
        var tree = CONVERTER.readTree("{\"type\": \"string\"} trailing garbage");

        // then
        var root = (Map<?, ?>) tree;
        assertThat(root.get("type")).isEqualTo("string");
    }
}
