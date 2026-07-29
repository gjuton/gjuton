package io.github.gjuton.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gjuton.internal.parser.SchemaParser;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumericSchemaTest {

    @Nested
    class ExclusiveBounds {

        @Test
        void booleanExclusiveMinimumResolvesToMinimumAsBound() {
            // when
            var schema = parseNumeric("""
                    {
                        "$schema": "http://json-schema.org/draft-04/schema#",
                        "type": "integer",
                        "minimum": 5,
                        "maximum": 10,
                        "exclusiveMinimum": true
                    }
                    """);

            // then
            assertThat(schema.getExclusiveMinimum()).isEqualByComparingTo(new BigDecimal("5"));
            assertThat(schema.getMaximum()).isEqualByComparingTo(new BigDecimal("10"));
        }

        @Test
        void booleanExclusiveMaximumResolvesToMaximumAsBound() {
            // when
            var schema = parseNumeric("""
                    {
                        "type": "number",
                        "minimum": 1.5,
                        "maximum": 10.5,
                        "exclusiveMaximum": true
                    }
                    """);

            // then
            assertThat(schema.getExclusiveMaximum()).isEqualByComparingTo(new BigDecimal("10.5"));
            assertThat(schema.getMinimum()).isEqualByComparingTo(new BigDecimal("1.5"));
        }

        @Test
        void falseExclusiveMinimumLeavesMinimumInclusive() {
            // when
            var schema = parseNumeric("""
                    {"type": "integer", "minimum": 5, "exclusiveMinimum": false}
                    """);

            // then
            assertThat(schema.getMinimum()).isEqualByComparingTo(new BigDecimal("5"));
            assertThat(schema.getExclusiveMinimum()).isNull();
        }

        @Test
        void booleanExclusiveMinimumWithoutMinimumIsDropped() {
            // when
            var schema = parseNumeric("""
                    {"type": "integer", "exclusiveMinimum": true}
                    """);

            // then
            assertThat(schema.getMinimum()).isNull();
            assertThat(schema.getExclusiveMinimum()).isNull();
        }

        @Test
        void booleanFormIsResolvedInNestedSchemas() {
            // when
            var root = (ObjectSchema) SchemaParser.parse("""
                    {
                        "type": "object",
                        "properties": {
                            "score": {"type": "integer", "minimum": 0, "exclusiveMinimum": true}
                        }
                    }
                    """).getRoot();

            // then
            var score = (NumericSchema) root.getProperties().get("score");
            assertThat(score.getExclusiveMinimum()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void numericExclusiveBoundsAreLeftUnchanged() {
            // when
            var schema = parseNumeric("""
                    {"type": "integer", "exclusiveMinimum": 5, "exclusiveMaximum": 10}
                    """);

            // then
            assertThat(schema.getExclusiveMinimum()).isEqualByComparingTo(new BigDecimal("5"));
            assertThat(schema.getExclusiveMaximum()).isEqualByComparingTo(new BigDecimal("10"));
        }
    }

    private static NumericSchema parseNumeric(String json) {
        return (NumericSchema) SchemaParser.parse(json).getRoot();
    }
}
