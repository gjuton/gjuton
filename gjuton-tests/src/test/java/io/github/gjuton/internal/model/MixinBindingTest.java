package io.github.gjuton.internal.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.gjuton.internal.parser.SchemaParser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pins every deserializer binding the schema model no longer declares
 * itself. One case per binding: a binding that goes missing degrades
 * quietly into a default-shaped value rather than failing, so each is
 * exercised with a payload only its deserializer maps correctly.
 */
class MixinBindingTest {

    private static <T extends Schema> T parse(String json, Class<T> type) {
        var document = SchemaParser.parse(json);
        return type.cast(document.getRoot());
    }

    // Schema

    @Test
    void allOfBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "allOf": [true]
                }
                """, Schema.class);

        // then
        assertThat(schema.getAllOf()).singleElement().isInstanceOf(UntypedSchema.class);
    }

    @Test
    void ifBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "if": true
                }
                """, Schema.class);

        // then
        assertThat(schema.getIfSchema()).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void thenBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "then": true
                }
                """, Schema.class);

        // then
        assertThat(schema.getThenSchema()).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void elseBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "else": true
                }
                """, Schema.class);

        // then
        assertThat(schema.getElseSchema()).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void notBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "not": true
                }
                """, Schema.class);

        // then
        assertThat(schema.getNotSchema()).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void oneOfBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "oneOf": [true]
                }
                """, Schema.class);

        // then
        assertThat(schema.getOneOf()).hasSize(1);
        assertThat(schema.getOneOf().get(0)).singleElement().isInstanceOf(UntypedSchema.class);
    }

    @Test
    void anyOfBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "anyOf": [true]
                }
                """, Schema.class);

        // then
        assertThat(schema.getAnyOf()).hasSize(1);
        assertThat(schema.getAnyOf().get(0)).singleElement().isInstanceOf(UntypedSchema.class);
    }

    // ObjectSchema

    @Test
    void propertiesBindThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "properties": {
                        "a": true
                    }
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getProperties()).containsOnlyKeys("a");
        assertThat(schema.getProperties().get("a")).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void patternPropertiesBindThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "patternProperties": {
                        "^a": true
                    }
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getPatternProperties().get("^a")).isInstanceOf(UntypedSchema.class);
    }

    @Test
    void additionalPropertiesFalseBindsToBoolean() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "additionalProperties": false
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getAdditionalProperties()).isEqualTo(Boolean.FALSE);
    }

    @Test
    void additionalPropertiesObjectBindsToSchema() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "additionalProperties": {
                        "type": "string"
                    }
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getAdditionalProperties()).isInstanceOf(StringSchema.class);
    }

    @Test
    void dependenciesBindToKeysFormAndSchemaForm() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "dependencies": {
                        "a": ["b"],
                        "c": {
                            "type": "string"
                        }
                    }
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getDependentRequired()).containsEntry("a", List.of("b"));
        assertThat(schema.getDependentSchemas().get("c")).isInstanceOf(StringSchema.class);
    }

    @Test
    void dependentSchemasBindThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "type": "object",
                    "dependentSchemas": {
                        "a": true
                    }
                }
                """, ObjectSchema.class);

        // then
        assertThat(schema.getDependentSchemas().get("a")).isInstanceOf(UntypedSchema.class);
    }

    // ArraySchema

    @Test
    void itemsAsTupleBindsToSchemaList() {
        // when
        var schema = parse("""
                {
                    "type": "array",
                    "items": [
                        {
                            "type": "string"
                        },
                        {
                            "type": "integer"
                        }
                    ]
                }
                """, ArraySchema.class);

        // then
        assertThat(schema.getPrefixSchemas()).hasSize(2);
        assertThat(schema.getPrefixSchemas().get(0)).isInstanceOf(StringSchema.class);
        assertThat(schema.getPrefixSchemas().get(1)).isInstanceOf(NumericSchema.class);
    }

    @Test
    void itemsAsObjectBindsToSchema() {
        // when
        var schema = parse("""
                {
                    "type": "array",
                    "items": {
                        "type": "string"
                    }
                }
                """, ArraySchema.class);

        // then
        assertThat(schema.getItemSchema()).isInstanceOf(StringSchema.class);
    }

    @Test
    void prefixItemsBindThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "type": "array",
                    "prefixItems": [true]
                }
                """, ArraySchema.class);

        // then
        assertThat(schema.getPrefixSchemas()).singleElement().isInstanceOf(UntypedSchema.class);
    }

    @Test
    void additionalItemsBindToSchema() {
        // when
        var schema = parse("""
                {
                    "type": "array",
                    "prefixItems": [true],
                    "additionalItems": {
                        "type": "string"
                    }
                }
                """, ArraySchema.class);

        // then
        assertThat(schema.getItemSchema()).isInstanceOf(StringSchema.class);
    }

    @Test
    void containsBindsThroughSchemaDeserializer() {
        // when
        var schema = parse("""
                {
                    "type": "array",
                    "contains": true
                }
                """, ArraySchema.class);

        // then
        assertThat(schema.getContains()).singleElement().isInstanceOf(UntypedSchema.class);
    }

    // NumericSchema

    @Test
    void exclusiveMinimumAsNumberBindsToBigDecimal() {
        // when
        var schema = parse("""
                {
                    "type": "integer",
                    "exclusiveMinimum": 5
                }
                """, NumericSchema.class);

        // then
        assertThat(schema.getExclusiveMinimum()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void exclusiveMinimumAsBooleanMarksMinimumExclusive() {
        // when
        var schema = parse("""
                {
                    "type": "integer",
                    "minimum": 5,
                    "exclusiveMinimum": true
                }
                """, NumericSchema.class);

        // then
        assertThat(schema.getExclusiveMinimum()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void exclusiveMaximumAsNumberBindsToBigDecimal() {
        // when
        var schema = parse("""
                {
                    "type": "integer",
                    "exclusiveMaximum": 7
                }
                """, NumericSchema.class);

        // then
        assertThat(schema.getExclusiveMaximum()).isEqualByComparingTo(BigDecimal.valueOf(7));
    }

    @Test
    void exclusiveMaximumAsBooleanMarksMaximumExclusive() {
        // when
        var schema = parse("""
                {
                    "type": "integer",
                    "maximum": 7,
                    "exclusiveMaximum": true
                }
                """, NumericSchema.class);

        // then
        assertThat(schema.getExclusiveMaximum()).isEqualByComparingTo(BigDecimal.valueOf(7));
    }

    // Both mappers

    @Test
    void refTargetBindsThroughTheRefResolversOwnMapper() {
        // when
        var document = SchemaParser.parse("""
                {
                    "$ref": "#/definitions/Bounded",
                    "definitions": {
                        "Bounded": {
                            "type": "integer",
                            "exclusiveMinimum": 5
                        }
                    }
                }
                """);

        // then
        var target = document.resolveRef("#/definitions/Bounded");
        assertThat(target).isInstanceOf(NumericSchema.class);
        var bounded = (NumericSchema) target;
        assertThat(bounded.getExclusiveMinimum()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }
}
