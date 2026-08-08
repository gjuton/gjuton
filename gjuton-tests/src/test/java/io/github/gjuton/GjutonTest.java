package io.github.gjuton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
import io.github.gjuton.errors.JsonBindingException;
import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.generator.GeneratorConfig;
import io.github.gjuton.internal.generator.GjutonMdc;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;

class GjutonTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String INT_SCHEMA = """
            { "type": "integer", "minimum": 5, "maximum": 100 }""";
    private static final String OBJECT_SCHEMA = """
            {
              "type": "object",
              "properties": { "a": { "type": "integer" } },
              "required": ["a"]
            }""";
    private static final String CLOSED_OBJECT_SCHEMA = """
            {
              "type": "object",
              "properties": { "a": { "type": "integer" } },
              "required": ["a"],
              "additionalProperties": false
            }""";
    private static final String RECURSIVE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "child": { "$ref": "#" },
                "v": { "type": "integer" }
              }
            }""";

    @Test
    void unconfiguredMatchesExplicitRandom() {
        // when
        var unconfigured = generate(Gjuton.of(OBJECT_SCHEMA).withSeed(42L), 50);
        var explicit = generate(
                Gjuton.of(OBJECT_SCHEMA).withGenerationMode(GenerationMode.RANDOM).withSeed(42L), 50);

        // then
        assertThat(unconfigured).isEqualTo(explicit);
    }

    @Test
    void exhaustiveModeEmitsBoundaryValuesFirst() {
        // when
        var gen = Gjuton.of(INT_SCHEMA).withGenerationMode(GenerationMode.EXHAUSTIVE).withSeed(1L);

        // then
        assertThat(gen.generate()).isEqualTo("5");
        assertThat(gen.generate()).isEqualTo("100");
    }

    @Test
    void randomModeSkipsBoundaryValues() {
        // when
        var gen = Gjuton.of(INT_SCHEMA).withGenerationMode(GenerationMode.RANDOM).withSeed(1L);
        var values = generate(gen, 100);

        // then
        assertThat(values.get(0)).isNotEqualTo("5");
        assertThat(values).allSatisfy(v -> assertThat(Integer.parseInt(v)).isBetween(5, 100));
    }

    @Test
    void withGenerationModeLastCallWins() {
        // when
        var gen = Gjuton.of(INT_SCHEMA)
                .withGenerationMode(GenerationMode.RANDOM)
                .withGenerationMode(GenerationMode.EXHAUSTIVE)
                .withSeed(1L);

        // then
        assertThat(gen.generate()).isEqualTo("5");
    }

    @Test
    void additionalPropertiesOffProducesOnlyDeclaredFields() {
        // when
        var gen = Gjuton.of(OBJECT_SCHEMA).withSeed(3L);
        var fieldNames = allFieldNames(gen, 100);

        // then
        assertThat(fieldNames).containsOnly("a");
    }

    @Test
    void additionalPropertiesOnAddsExtraFields() {
        // when
        var gen = Gjuton.of(OBJECT_SCHEMA).withAdditionalProperties().withSeed(3L);
        var fieldNames = allFieldNames(gen, 100);

        // then
        assertThat(fieldNames).contains("a");
        assertThat(fieldNames).hasSizeGreaterThan(1);
    }

    @Test
    void additionalPropertiesOnHonoursAdditionalPropertiesFalse() {
        // when
        var gen = Gjuton.of(CLOSED_OBJECT_SCHEMA).withAdditionalProperties().withSeed(3L);
        var fieldNames = allFieldNames(gen, 100);

        // then
        assertThat(fieldNames).containsOnly("a");
    }

    @Test
    void deepNestingLimitsAllowDeeperNestingThanShallow() {
        // when
        int shallow = maxNestingDepth(
                Gjuton.of(RECURSIVE_SCHEMA).withNestingLimitsShallow().withSeed(9L));
        int deep = maxNestingDepth(
                Gjuton.of(RECURSIVE_SCHEMA).withNestingLimitsDeep().withSeed(9L));

        // then
        assertThat(deep).isGreaterThan(shallow);
    }

    @Test
    void withGenerationModeRejectsNull() {
        // then
        assertThatThrownBy(() -> Gjuton.of(INT_SCHEMA).withGenerationMode(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void factoriesRejectNullSchema() {
        // then
        assertThatThrownBy(() -> Gjuton.of((String) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gjuton.of((File) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Gjuton.of((InputStream) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generateToTargetRejectsNull() {
        // when
        var gen = Gjuton.of(INT_SCHEMA);

        // then
        assertThatThrownBy(() -> gen.generate((OutputStream) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gen.generate((Writer) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> gen.generate((File) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withNestingLimitsRejectsSoftBelowOne() {
        // then
        assertThatThrownBy(() -> Gjuton.of(INT_SCHEMA).withNestingLimits(0, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void withNestingLimitsRejectsSoftAboveHard() {
        // then
        assertThatThrownBy(() -> Gjuton.of(INT_SCHEMA).withNestingLimits(5, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<String> generate(Gjuton gen, int iterations) {
        var values = new ArrayList<String>(iterations);
        for (int i = 0; i < iterations; i++) {
            values.add(gen.generate());
        }
        return values;
    }

    private static List<String> allFieldNames(Gjuton gen, int iterations) {
        var names = new ArrayList<String>();
        for (int i = 0; i < iterations; i++) {
            var node = parse(gen.generate());
            node.fieldNames().forEachRemaining(names::add);
        }
        return names;
    }

    private static int maxNestingDepth(Gjuton gen) {
        int max = 0;
        for (int i = 0; i < 200; i++) {
            max = Math.max(max, nestingDepth(parse(gen.generate())));
        }
        return max;
    }

    private static int nestingDepth(JsonNode node) {
        var child = node.get("child");
        return child == null ? 0 : 1 + nestingDepth(child);
    }

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    class OverridesByPath {

        private static final String TWO_FIELD_SCHEMA = """
                {
                  "type": "object",
                  "properties": { "role": { "type": "string" }, "n": { "type": "integer" } },
                  "required": ["role", "n"]
                }""";
        private static final String NESTED_SCHEMA = """
                {
                  "type": "object",
                  "properties": {
                    "a": {
                      "type": "object",
                      "properties": { "b": { "type": "string" } },
                      "required": ["b"]
                    }
                  },
                  "required": ["a"]
                }""";
        private static final String ARRAY_SCHEMA = """
                { "type": "array", "items": { "type": "integer" }, "minItems": 3 }""";

        @Test
        void overrideReplacesFieldValue() {
            // when
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.role", () -> "admin");

            // then
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("admin");
        }

        @Test
        void overrideInvokedOnEachGenerate() {
            // when
            var counter = new int[] {0};
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.role", () -> "user-" + counter[0]++);

            // then
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("user-0");
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("user-1");
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("user-2");
        }

        @Test
        void overrideReturningBeanSerializesAsObject() {
            // when
            var gen = Gjuton.of(NESTED_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.a", () -> new Point(3, 4));

            // then
            var a = parse(gen.generate()).get("a");
            assertThat(a.get("x").asInt()).isEqualTo(3);
            assertThat(a.get("y").asInt()).isEqualTo(4);
        }

        @Test
        void overrideOnNestedPathOverridesOnlyThatField() {
            // when
            var gen = Gjuton.of(NESTED_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.a.b", () -> "fixed");

            // then
            assertThat(parse(gen.generate()).get("a").get("b").asText()).isEqualTo("fixed");
        }

        @Test
        void overrideOnArrayElementOverridesThatIndex() {
            // when
            var gen = Gjuton.of(ARRAY_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$[0]", () -> 999);

            // then
            assertThat(parse(gen.generate()).get(0).asInt()).isEqualTo(999);
        }

        @Test
        void overrideAtRootReplacesWholeValue() {
            // when
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$", () -> List.of("replaced"));

            // then
            var root = parse(gen.generate());
            assertThat(root.isArray()).isTrue();
            assertThat(root.get(0).asText()).isEqualTo("replaced");
        }

        @Test
        void overrideBypassesGenerationOfUnsatisfiableField() {
            // given a schema whose required field can never be generated
            var schema = """
                    { "type": "object", "properties": { "x": false }, "required": ["x"] }""";

            // then generation fails without an override
            assertThatThrownBy(() -> Gjuton.of(schema).withSeed(1L).generate())
                    .isInstanceOf(UnsatisfiableSchemaException.class);

            // when an override supplies the value, the subtree is never generated
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByPath("$.x", () -> "supplied");

            // then
            assertThat(parse(gen.generate()).get("x").asText()).isEqualTo("supplied");
        }

        @Test
        void overrideBypassesRequiredFieldWithNoSchema() {
            // given a required field the schema neither declares nor allows
            var schema = """
                    { "type": "object", "required": ["x"], "additionalProperties": false }""";

            // then generation fails without an override
            assertThatThrownBy(() -> Gjuton.of(schema).withSeed(1L).generate())
                    .isInstanceOf(UnsatisfiableSchemaException.class);

            // when an override supplies the value, the field is never resolved
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByPath("$.x", () -> "supplied");

            // then
            assertThat(parse(gen.generate()).get("x").asText()).isEqualTo("supplied");
        }

        @Test
        void overriddenValueIsNotValidatedAgainstItsSchema() {
            // given a validate-and-retry parent (anyOf) around an integer field,
            // overridden with a non-integer value
            var schema = """
                    {
                      "anyOf": [
                        { "type": "object", "properties": { "n": { "type": "integer" } }, "required": ["n"] }
                      ]
                    }""";
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByPath("$.n", () -> "not-a-number");

            // then the override survives validation and appears verbatim
            assertThat(parse(gen.generate()).get("n").asText()).isEqualTo("not-a-number");
        }

        @Test
        void overrideOnUnvisitedPathNeverFires() {
            // when an override targets a field the schema does not declare
            var fired = new boolean[] {false};
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.absent", () -> {
                        fired[0] = true;
                        return "x";
                    });
            gen.generate();

            // then it is never invoked
            assertThat(fired[0]).isFalse();
        }

        @Test
        void withOverrideByPathLastCallWinsForSamePath() {
            // when
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByPath("$.role", () -> "first")
                    .withOverrideByPath("$.role", () -> "second");

            // then
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("second");
        }

        @Test
        void withOverrideByPathRejectsNullArguments() {
            // then
            assertThatThrownBy(() -> Gjuton.of(TWO_FIELD_SCHEMA).withOverrideByPath(null, () -> "x"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Gjuton.of(TWO_FIELD_SCHEMA).withOverrideByPath("$.role", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        private record Point(int x, int y) {
        }
    }

    @Nested
    class OverridesByName {

        private static final String TWO_FIELD_SCHEMA = """
                {
                  "type": "object",
                  "properties": { "role": { "type": "string" }, "n": { "type": "integer" } },
                  "required": ["role", "n"]
                }""";

        @Test
        void overrideByNameMatchesAtMultiplePositions() {
            // given a schema with the same property name at two different paths
            var schema = """
                    {
                      "type": "object",
                      "properties": {
                        "id": { "type": "string" },
                        "child": {
                          "type": "object",
                          "properties": { "id": { "type": "string" } },
                          "required": ["id"]
                        }
                      },
                      "required": ["id", "child"]
                    }""";

            // when
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByName("id", () -> "fixed-id");

            // then
            var root = parse(gen.generate());
            assertThat(root.get("id").asText()).isEqualTo("fixed-id");
            assertThat(root.get("child").get("id").asText()).isEqualTo("fixed-id");
        }

        @Test
        void overrideByNameSharesValueAcrossPositionsWithinOneGenerate() {
            // given a schema with the same property name at two different paths
            var schema = """
                    {
                      "type": "object",
                      "properties": {
                        "id": { "type": "string" },
                        "child": {
                          "type": "object",
                          "properties": { "id": { "type": "string" } },
                          "required": ["id"]
                        }
                      },
                      "required": ["id", "child"]
                    }""";

            // when — a counter proves the override fires once per generate(), not per position
            var counter = new int[] {0};
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByName("id", () -> "id-" + counter[0]++);

            // then — both positions share the same value within one generate() call
            var root = parse(gen.generate());
            assertThat(root.get("id").asText()).isEqualTo("id-0");
            assertThat(root.get("child").get("id").asText()).isEqualTo("id-0");

            // and a second generate() call gets a fresh value
            var root2 = parse(gen.generate());
            assertThat(root2.get("id").asText()).isEqualTo("id-1");
            assertThat(root2.get("child").get("id").asText()).isEqualTo("id-1");
        }

        @Test
        void pathOverrideTakesPrecedenceOverNameOverride() {
            // when
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByName("role", () -> "by-name")
                    .withOverrideByPath("$.role", () -> "by-path");

            // then
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("by-path");
        }

        @Test
        void overrideByNameDoesNotMatchArrayElements() {
            var schema = """
                    { "type": "array", "items": { "type": "integer" }, "minItems": 2 }""";

            // when — register a name that happens to be the string "0"
            var fired = new boolean[] {false};
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByName("0", () -> {
                        fired[0] = true;
                        return 999;
                    });
            gen.generate();

            // then
            assertThat(fired[0]).isFalse();
        }

        @Test
        void withOverrideByNameLastCallWinsForSameName() {
            // when
            var gen = Gjuton.of(TWO_FIELD_SCHEMA).withSeed(1L)
                    .withOverrideByName("role", () -> "first")
                    .withOverrideByName("role", () -> "second");

            // then
            assertThat(parse(gen.generate()).get("role").asText()).isEqualTo("second");
        }

        @Test
        void withOverrideByNameRejectsNullArguments() {
            // then
            assertThatThrownBy(() -> Gjuton.of(TWO_FIELD_SCHEMA).withOverrideByName(null, () -> "x"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Gjuton.of(TWO_FIELD_SCHEMA).withOverrideByName("role", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class OverridesByFormat {

        private static final String TWO_FORMATS_SCHEMA = """
                {
                  "type": "object",
                  "properties": {
                    "createdAt": { "type": "string", "format": "date-time" },
                    "updatedAt": { "type": "string", "format": "date-time" },
                    "account": { "type": "string", "format": "iban" },
                    "label": { "type": "string" }
                  },
                  "required": ["createdAt", "updatedAt", "account", "label"]
                }""";

        @Test
        void overrideByFormatAppliesAtEveryStringOfThatFormat() {
            // when
            var gen = Gjuton.of(TWO_FORMATS_SCHEMA).withSeed(1L)
                    .withOverrideByFormat("date-time", () -> "2024-01-01T00:00:00Z");

            // then — only the date-times are replaced
            var root = parse(gen.generate());
            assertThat(root.get("createdAt").asText()).isEqualTo("2024-01-01T00:00:00Z");
            assertThat(root.get("updatedAt").asText()).isEqualTo("2024-01-01T00:00:00Z");
            assertThat(root.get("label").asText()).isNotEqualTo("2024-01-01T00:00:00Z");
        }

        @Test
        void overrideByFormatAppliesToFormatGjutonDoesNotModel() {
            // when
            var gen = Gjuton.of(TWO_FORMATS_SCHEMA).withSeed(1L)
                    .withOverrideByFormat("iban", () -> "SE3550000000054910000003");

            // then
            assertThat(parse(gen.generate()).get("account").asText()).isEqualTo("SE3550000000054910000003");
        }

        @Test
        void overrideByFormatProducesAnIndependentValuePerPosition() {
            // when — a counter proves the override fires once per matching position
            var counter = new int[] {0};
            var gen = Gjuton.of(TWO_FORMATS_SCHEMA).withSeed(1L)
                    .withOverrideByFormat("date-time", () -> "at-" + counter[0]++);

            // then
            var root = parse(gen.generate());
            assertThat(List.of(root.get("createdAt").asText(), root.get("updatedAt").asText()))
                    .containsExactlyInAnyOrder("at-0", "at-1");
        }

        @Test
        void overrideByFormatMatchesWhenTheStringTypeIsOnlyImplied() {
            // given a schema that names a format without declaring "type": "string"
            var schema = """
                    { "type": "object", "properties": { "a": { "format": "iban" } }, "required": ["a"] }""";

            // when
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByFormat("iban", () -> "SE35");

            // then
            assertThat(parse(gen.generate()).get("a").asText()).isEqualTo("SE35");
        }

        @Test
        void withOverrideByFormatLastCallWinsForSameFormat() {
            // when
            var gen = Gjuton.of(TWO_FORMATS_SCHEMA).withSeed(1L)
                    .withOverrideByFormat("iban", () -> "first")
                    .withOverrideByFormat("iban", () -> "second");

            // then
            assertThat(parse(gen.generate()).get("account").asText()).isEqualTo("second");
        }

        @Test
        void withOverrideByFormatRejectsNullArguments() {
            // then
            assertThatThrownBy(() -> Gjuton.of(TWO_FORMATS_SCHEMA).withOverrideByFormat(null, () -> "x"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Gjuton.of(TWO_FORMATS_SCHEMA).withOverrideByFormat("iban", null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void mostSpecificOverrideWinsAcrossAllThreeKinds() {
            var schema = """
                    {
                      "type": "object",
                      "properties": {
                        "byPath": { "type": "string", "format": "date-time" },
                        "byName": { "type": "string", "format": "date-time" },
                        "byFormat": { "type": "string", "format": "date-time" }
                      },
                      "required": ["byPath", "byName", "byFormat"]
                    }""";

            // when — every position is matched by its own kind and all less specific ones
            var gen = Gjuton.of(schema).withSeed(1L)
                    .withOverrideByPath("$.byPath", () -> "path")
                    .withOverrideByName("byName", () -> "name")
                    .withOverrideByFormat("date-time", () -> "format");

            // then
            var root = parse(gen.generate());
            assertThat(root.get("byPath").asText()).isEqualTo("path");
            assertThat(root.get("byName").asText()).isEqualTo("name");
            assertThat(root.get("byFormat").asText()).isEqualTo("format");
        }

        @Test
        void overridesAreDeterministicForFixedSeed() {
            // when
            Supplier<Gjuton> generator = () -> Gjuton.of(TWO_FORMATS_SCHEMA).withSeed(7L)
                    .withOverrideByFormat("iban", () -> "fixed-iban");

            // then
            assertThat(generator.get().generate()).isEqualTo(generator.get().generate());
        }
    }

    @Nested
    class GenerateIntoType {

        private record Bean(int a) {
        }

        @Test
        void bindsGeneratedValueIntoPojo() {
            // given a schema whose integer field fits the target's int component
            var schema = """
                    {
                      "type": "object",
                      "properties": { "a": { "type": "integer", "minimum": 5, "maximum": 100 } },
                      "required": ["a"]
                    }""";

            // when
            var bean = Gjuton.of(schema).withSeed(1L).generate(Bean.class);

            // then
            assertThat(bean.a()).isBetween(5, 100);
        }

        @Test
        void bindingFailureThrowsJsonBindingException() {
            // given a schema whose generated field type cannot map onto the target
            var schema = """
                    {
                      "type": "object",
                      "properties": { "a": { "type": "array", "items": { "type": "integer" } } },
                      "required": ["a"]
                    }""";

            // then
            assertThatThrownBy(() -> Gjuton.of(schema).withSeed(1L).generate(Bean.class))
                    .isInstanceOf(JsonBindingException.class);
        }

        @Test
        void matchesDeserializingGenerateOutput() {
            // given two generators with the same schema and seed
            var schema = """
                    {
                      "type": "object",
                      "properties": { "a": { "type": "integer", "minimum": 5, "maximum": 100 } },
                      "required": ["a"]
                    }""";
            var stringGen = Gjuton.of(schema).withSeed(1L);
            var typedGen = Gjuton.of(schema).withSeed(1L);

            // when
            var fromString = parse(stringGen.generate());
            var fromTyped = typedGen.generate(Bean.class);

            // then
            assertThat(fromTyped.a()).isEqualTo(fromString.get("a").asInt());
        }
    }

    @Nested
    class NoveltyScore {

        @Test
        void startsAtOneBeforeAnyGeneration() {
            // when
            var gen = Gjuton.of(INT_SCHEMA).withGenerationMode(GenerationMode.EXHAUSTIVE).withSeed(1L);

            // then
            assertThat(gen.noveltyScore()).isEqualTo(1.0);
        }

        @Test
        void doesNotThrowInRandomMode() {
            // when
            var gen = Gjuton.of(INT_SCHEMA).withGenerationMode(GenerationMode.RANDOM).withSeed(1L);
            gen.generate();

            // then
            assertThat(gen.noveltyScore()).isEqualTo(1.0);
        }

        @Test
        void dropsAsTheSameValuesRepeat() {
            var gen = Gjuton.of("""
                    { "enum": ["a", "b", "c"] }""").withGenerationMode(GenerationMode.EXHAUSTIVE).withSeed(1L);

            // when: each of the three literals is novel in turn
            gen.generate();
            gen.generate();
            gen.generate();

            // then
            assertThat(gen.noveltyScore()).isEqualTo(1.0);

            // when: two more calls can only repeat an already-seen literal
            gen.generate();
            gen.generate();

            // then: those two non-novel calls pull the score down
            assertThat(gen.noveltyScore()).isEqualTo(0.6);
        }
    }

    /**
     * An enum's deliberate value set is every literal. These tests assert the
     * generator actually emits all of them when the enum is nested behind each
     * kind of construct, checking the generated output rather than the coverage
     * measure. The literals are distinctive so random values of other branches
     * cannot masquerade as them.
     */
    @Nested
    class EnumExhaustiveness {

        private static final String[] ENUM_VALUES = {"enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"};

        @Test
        void emitsAllEnumValuesBehindOptionalProperty() {
            assertEmitsAllEnumValues("""
                    {
                      "type": "object",
                      "properties": { "p": { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] } }
                    }""");
        }

        @Test
        void emitsAllEnumValuesBehindIfThen() {
            assertEmitsAllEnumValues("""
                    {
                      "if": { "properties": { "kind": { "const": "match" } }, "required": ["kind"] },
                      "then": {
                        "properties": { "e": { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] } },
                        "required": ["kind", "e"]
                      }
                    }""");
        }

        @Test
        void emitsAllEnumValuesBehindElse() {
            assertEmitsAllEnumValues("""
                    {
                      "if": { "properties": { "kind": { "const": "match" } }, "required": ["kind"] },
                      "then": { "type": "object" },
                      "else": {
                        "properties": { "e": { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] } },
                        "required": ["e"]
                      }
                    }""");
        }

        @Test
        void emitsAllEnumValuesInsideArray() {
            assertEmitsAllEnumValues("""
                    {
                      "type": "array",
                      "items": { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] },
                      "minItems": 1
                    }""");
        }

        @Test
        void emitsAllEnumValuesBehindOneOf() {
            assertEmitsAllEnumValues("""
                    {
                      "oneOf": [
                        { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] },
                        { "type": "integer" }
                      ]
                    }""");
        }

        @Test
        void emitsAllEnumValuesBehindAnyOf() {
            assertEmitsAllEnumValues("""
                    {
                      "anyOf": [
                        { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] },
                        { "type": "string" }
                      ]
                    }""");
        }

        @Test
        @Disabled("#121 — the enum branch over-matches the string branch, so every enum "
                + "value fails the exactly-one oneOf rule and is discarded; the values are "
                + "swallowed and never reach output, yet the enum's own phases still advance "
                + "as if each one had been emitted")
        void emitsAllEnumValuesBehindOverMatchingOneOf() {
            assertEmitsAllEnumValues("""
                    {
                      "oneOf": [
                        { "enum": ["enumAlphaXQ7", "enumBravoXQ7", "enumCharlieXQ7"] },
                        { "type": "string" }
                      ]
                    }""");
        }

        private void assertEmitsAllEnumValues(String schema) {
            var gen = Gjuton.of(schema).withGenerationMode(GenerationMode.EXHAUSTIVE).withSeed(1L);

            // when
            var seen = new HashSet<String>();
            for (int i = 0; i < 1000 && seen.size() < ENUM_VALUES.length; i++) {
                var output = gen.generate();
                for (var value : ENUM_VALUES) {
                    if (output.contains("\"" + value + "\"")) {
                        seen.add(value);
                    }
                }
            }

            // then
            assertThat(seen).containsExactlyInAnyOrder(ENUM_VALUES);
        }
    }

    // Remove this test class, its not real behaviour, its just a hack for the integration tests
    @Nested
    class RunId {

        @Test
        void suppliesRunIdForTheDurationOfGenerationWhenTheCallerHasNone() {
            // given
            var observed = new ArrayList<String>();
            var gen = Gjuton.of(OBJECT_SCHEMA)
                    .withOverrideByName("a", () -> {
                        observed.add(MDC.get(GjutonMdc.RUN_ID_KEY));
                        return 1;
                    });

            // when
            gen.generate();
            gen.generate();

            // then
            assertThat(observed).hasSize(2).doesNotContainNull();
            assertThat(observed.get(0)).isNotEqualTo(observed.get(1));
            assertThat(MDC.get(GjutonMdc.RUN_ID_KEY)).isNull();
        }

        @Test
        void keepsTheCallersRunIdForTheDurationOfGeneration() {
            // given
            var observed = new ArrayList<String>();
            var gen = Gjuton.of(OBJECT_SCHEMA)
                    .withOverrideByName("a", () -> {
                        observed.add(MDC.get(GjutonMdc.RUN_ID_KEY));
                        return 1;
                    });
            MDC.put(GjutonMdc.RUN_ID_KEY, "caller-owned");

            // when
            gen.generate();

            // then
            assertThat(observed).containsExactly("caller-owned");
            assertThat(MDC.get(GjutonMdc.RUN_ID_KEY)).isNull();
        }
    }

    @Nested
    class TraceContext {

        @Test
        void labelsGenerationWithTheSeedAndModeItWasGiven() {
            // given
            var observed = new ArrayList<String>();
            var gen = Gjuton.of(OBJECT_SCHEMA)
                    .withSeed(99L)
                    .withGenerationMode(GenerationMode.RANDOM)
                    .withOverrideByName("a", () -> {
                        observed.add(MDC.get(GjutonMdc.SEED_KEY) + "/" + MDC.get(GjutonMdc.MODE_KEY));
                        return 1;
                    });

            // when
            gen.generate();
            gen.generate();

            // then
            assertThat(observed).containsExactly("supplied-99/RANDOM", "supplied-99/RANDOM");
            assertThat(MDC.get(GjutonMdc.SEED_KEY)).isNull();
            assertThat(MDC.get(GjutonMdc.MODE_KEY)).isNull();
        }

        @Test
        void generatesSeedForRunGivenNone() {
            // given
            var observed = new ArrayList<String>();
            var gen = Gjuton.of(OBJECT_SCHEMA)
                    .withOverrideByName("a", () -> {
                        observed.add(MDC.get(GjutonMdc.SEED_KEY));
                        return 1;
                    });

            // when
            gen.generate();

            // then
            assertThat(observed).singleElement().asString().matches("random--?\\d+");
        }

    }

    /**
     * Depth is nesting in the generated value, so a schema factored into one
     * definition per level generates what the same schema written inline does.
     * Recursion through an omittable position bottoms out at the soft limit;
     * recursion with no way out runs into the hard limit.
     */
    @Nested
    class NestingLimits {

        private static final String REF_CHAIN_FIVE_LEVELS = """
                {
                  "type": "object",
                  "properties": { "l1": { "$ref": "#/$defs/L1" } },
                  "required": ["l1"],
                  "$defs": {
                    "L1": { "type": "object", "properties": { "l2": { "$ref": "#/$defs/L2" } }, "required": ["l2"] },
                    "L2": { "type": "object", "properties": { "l3": { "$ref": "#/$defs/L3" } }, "required": ["l3"] },
                    "L3": { "type": "object", "properties": { "l4": { "$ref": "#/$defs/L4" } }, "required": ["l4"] },
                    "L4": { "type": "object", "properties": { "leaf": { "type": "string" } }, "required": ["leaf"] }
                  }
                }""";
        private static final String INLINE_FIVE_LEVELS = """
                {
                  "type": "object",
                  "properties": {
                    "l1": { "type": "object", "properties": {
                      "l2": { "type": "object", "properties": {
                        "l3": { "type": "object", "properties": {
                          "l4": { "type": "object", "properties": {
                            "leaf": { "type": "string" }
                          }, "required": ["leaf"] }
                        }, "required": ["l4"] }
                      }, "required": ["l3"] }
                    }, "required": ["l2"] }
                  },
                  "required": ["l1"]
                }""";
        private static final String NESTED_ARRAYS = """
                {
                  "type": "array",
                  "minItems": 1,
                  "maxItems": 2,
                  "items": {
                    "type": "array",
                    "minItems": 1,
                    "maxItems": 2,
                    "items": { "type": "array", "minItems": 1, "maxItems": 2, "items": { "type": "integer" } }
                  }
                }""";
        private static final String OPTIONAL_SELF_REFERENCE = """
                {
                  "$ref": "#/$defs/Node",
                  "$defs": {
                    "Node": {
                      "type": "object",
                      "additionalProperties": false,
                      "properties": {
                        "name": { "type": "string" },
                        "next": { "$ref": "#/$defs/Node" }
                      }
                    }
                  }
                }""";
        private static final String SELF_REFERENCE_REQUIRED = """
                {
                  "$ref": "#/$defs/A",
                  "$defs": {
                    "A": {
                      "type": "object",
                      "required": ["a"],
                      "properties": { "a": { "$ref": "#/$defs/A" } }
                    }
                  }
                }""";
        private static final String MUTUAL_REFERENCE_REQUIRED = """
                {
                  "$ref": "#/$defs/A",
                  "$defs": {
                    "A": { "type": "object", "required": ["b"], "properties": { "b": { "$ref": "#/$defs/B" } } },
                    "B": { "type": "object", "required": ["a"], "properties": { "a": { "$ref": "#/$defs/A" } } }
                  }
                }""";
        /**
         * The only branch of a required property names the definition it sits
         * under, which no {@code RefGenerator} ever expands.
         */
        private static final String SELF_REFERENCE_THROUGH_BRANCH = """
                {
                  "$ref": "#/$defs/A",
                  "$defs": {
                    "A": {
                      "type": "object",
                      "required": ["a"],
                      "properties": {
                        "a": {
                          "oneOf": [
                            {
                              "$ref": "#/$defs/A"
                            }
                          ]
                        }
                      }
                    }
                  }
                }""";

        @Test
        void chainOfDefinitionsGeneratesEveryLevelItSpans() {
            // when
            var json = Gjuton.of(REF_CHAIN_FIVE_LEVELS).withSeed(1L).generate();
            var root = parse(json);

            // then
            var leaf = root.get("l1").get("l2").get("l3").get("l4").get("leaf");
            assertThat(leaf.isTextual()).isTrue();
        }

        @Test
        void chainOfDefinitionsMatchesTheEquivalentInlineSchema() {
            // when
            var factored = Gjuton.of(REF_CHAIN_FIVE_LEVELS).withSeed(1L).generate();
            var inline = Gjuton.of(INLINE_FIVE_LEVELS).withSeed(1L).generate();

            // then
            var factoredShape = shape(parse(factored));
            var inlineShape = shape(parse(inline));
            assertThat(factoredShape).isEqualTo(inlineShape);
        }

        @Test
        void tenDefinitionsEachAddingOneLevelGenerateToFullDepthUnderTheDeepPreset() {
            // when
            var gen = Gjuton.of(chainOfDefinitions(10)).withNestingLimitsDeep().withSeed(1L);
            var node = parse(gen.generate());

            // then
            for (int level = 1; level <= 10; level++) {
                node = node.get("l" + level);
                assertThat(node).as("level l%d", level).isNotNull();
            }
            assertThat(node.get("leaf").isTextual()).isTrue();
        }

        @Test
        void everyArrayElementLevelCountsTowardDepth() {
            // when — three nested array levels fit under the default hard limit,
            // which they would not if only $ref expansions were counted against it
            var json = Gjuton.of(NESTED_ARRAYS).withSeed(1L).generate();
            var root = parse(json);

            // then
            assertThat(root.get(0).get(0).get(0).isInt()).isTrue();
        }

        @Test
        void requiredChainDeeperThanTheHardLimitReportsTheNestingLimit() {
            // when — the shallow preset's hard limit is the cheapest one to exceed
            var gen = Gjuton.of(chainOfDefinitions(GeneratorConfig.SHALLOW_HARD_NESTING_DEPTH + 1))
                    .withNestingLimitsShallow()
                    .withSeed(1L);

            // then
            assertThatThrownBy(gen::generate)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("nesting limit of " + GeneratorConfig.SHALLOW_HARD_NESTING_DEPTH)
                    .hasMessageNotContaining("infinite recursion");
        }

        @Test
        void recursiveOptionalSchemaCollapsesAtTheSoftLimit() {
            // when
            int depth = maxNestingDepth(Gjuton.of(RECURSIVE_SCHEMA).withSeed(9L));

            // then
            assertThat(depth).isLessThanOrEqualTo(GeneratorConfig.DEFAULT_SOFT_NESTING_DEPTH);
        }

        @Test
        void longFiniteAllOfInheritanceChainGeneratesEveryPropertyItInherits() {
            // given seven definitions inheriting from each other through allOf,
            // none of which adds a value level
            var gen = Gjuton.of("""
                    {
                      "$ref": "#/$defs/L7",
                      "$defs": {
                        "L1": { "type": "object", "properties": { "p1": { "type": "string" } }, "required": ["p1"] },
                        "L2": { "allOf": [ { "$ref": "#/$defs/L1" } ], "properties": { "p2": { "type": "string" } }, "required": ["p2"] },
                        "L3": { "allOf": [ { "$ref": "#/$defs/L2" } ], "properties": { "p3": { "type": "string" } }, "required": ["p3"] },
                        "L4": { "allOf": [ { "$ref": "#/$defs/L3" } ], "properties": { "p4": { "type": "string" } }, "required": ["p4"] },
                        "L5": { "allOf": [ { "$ref": "#/$defs/L4" } ], "properties": { "p5": { "type": "string" } }, "required": ["p5"] },
                        "L6": { "allOf": [ { "$ref": "#/$defs/L5" } ], "properties": { "p6": { "type": "string" } }, "required": ["p6"] },
                        "L7": { "allOf": [ { "$ref": "#/$defs/L6" } ], "properties": { "p7": { "type": "string" } }, "required": ["p7"] }
                      }
                    }""").withSeed(1L);

            // when
            var generated = parse(gen.generate());

            // then
            assertThat(generated.get("p1").isTextual()).isTrue();
            assertThat(generated.get("p7").isTextual()).isTrue();
        }

        @Test
        void longFiniteChainOfBranchReferencesGenerates() {
            // given six definitions each reaching the next through a single
            // oneOf branch, none of which adds a value level
            var gen = Gjuton.of("""
                    {
                      "$ref": "#/$defs/B6",
                      "$defs": {
                        "B1": { "type": "object", "properties": { "leaf": { "type": "string" } }, "required": ["leaf"] },
                        "B2": { "type": "object", "oneOf": [ { "$ref": "#/$defs/B1" } ] },
                        "B3": { "type": "object", "oneOf": [ { "$ref": "#/$defs/B2" } ] },
                        "B4": { "type": "object", "oneOf": [ { "$ref": "#/$defs/B3" } ] },
                        "B5": { "type": "object", "oneOf": [ { "$ref": "#/$defs/B4" } ] },
                        "B6": { "type": "object", "oneOf": [ { "$ref": "#/$defs/B5" } ] }
                      }
                    }""").withSeed(1L);

            // when
            var generated = parse(gen.generate());

            // then
            assertThat(generated.get("leaf").isTextual()).isTrue();
        }

        @Test
        void selfReferencingAdditionalPropertiesFillTerminates() {
            // given a schema whose every property must itself be one of these
            // objects, and which must have at least one property
            var gen = Gjuton.of("""
                    {
                      "$ref": "#/$defs/Node",
                      "$defs": {
                        "Node": {
                          "type": "object",
                          "minProperties": 1,
                          "additionalProperties": { "$ref": "#/$defs/Node" }
                        }
                      }
                    }""").withNestingLimitsShallow().withSeed(1L);

            // when
            var thrown = catchThrowable(gen::generate);

            // then
            assertThat(thrown)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("nesting limit");
        }

        @Test
        void anOptionalSelfReferenceGeneratesFiniteDocument() {
            // when
            var values = generate(Gjuton.of(OPTIONAL_SELF_REFERENCE).withSeed(1L), 20);

            // then — every run bottoms out, and the recursion is still explored
            assertThat(values).allSatisfy(value -> assertThat(parse(value).isObject()).isTrue());
            assertThat(values).anySatisfy(value -> assertThat(parse(value).has("next")).isTrue());
        }

        @Test
        void anOptionalSelfReferenceForcedInByMinPropertiesGeneratesFiniteDocument() {
            // given a definition whose only self-referencing property is
            // optional, but which must carry at least one property, so
            // generation selects an optional property even at its smallest
            var schema = OPTIONAL_SELF_REFERENCE.replace("\"type\": \"object\",", "\"type\": \"object\", \"minProperties\": 1,");

            // when
            var values = generate(Gjuton.of(schema).withSeed(1L), 20);

            // then
            assertThat(values).allSatisfy(value -> assertThat(parse(value).size()).isGreaterThanOrEqualTo(1));
            assertThat(values).anySatisfy(value -> assertThat(parse(value).has("next")).isTrue());
        }

        @Test
        void recursiveSchemaBottomingOutThroughAnEmptyArrayGenerates() {
            // given a required property whose array may be empty
            var gen = Gjuton.of("""
                    {
                      "$ref": "#/$defs/Node",
                      "$defs": {
                        "Node": {
                          "type": "object",
                          "required": ["children"],
                          "properties": {
                            "children": { "type": "array", "minItems": 0, "items": { "$ref": "#/$defs/Node" } }
                          }
                        }
                      }
                    }""").withSeed(1L);

            // when
            var values = generate(gen, 20);

            // then
            assertThat(values).allSatisfy(value -> assertThat(parse(value).get("children").isArray()).isTrue());
        }

        @ParameterizedTest
        @ValueSource(strings = {SELF_REFERENCE_REQUIRED, MUTUAL_REFERENCE_REQUIRED, SELF_REFERENCE_THROUGH_BRANCH})
        void recursionWithNoWayOutRunsIntoTheNestingLimit(String schema) {
            // when
            var thrown = catchThrowable(() -> Gjuton.of(schema).withSeed(1L).generate());

            // then the message names the limit, whether the recursion ran out
            // of levels or spent rounds faster than it produced them
            assertThat(thrown)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("configured nesting limit of 10 levels");
        }

        @Test
        void branchLeadingOutOfTheCycleBottomsOutEveryValue() throws IOException {
            // given one branch re-enters the definition, the other does not
            var schema = GjutonTest.class.getResourceAsStream("/schemas/recursive-required-oneof-escape.json");

            // when
            var values = generate(Gjuton.of(schema).withSeed(1L), 20);

            // then — however deep the recursive branch is taken, the escape
            // branch ends it
            assertThat(values).allSatisfy(value -> {
                var node = parse(value).get("node");
                while (node.isObject()) {
                    node = node.get("node");
                }
                assertThat(node.isTextual()).isTrue();
            });
        }

        /**
         * A schema of {@code levels} definitions, each adding one required
         * object level, with a string leaf inside the innermost one.
         */
        private static String chainOfDefinitions(int levels) {
            var defs = new StringBuilder();
            for (int level = 1; level <= levels; level++) {
                var body = level == levels
                        ? "{ \"type\": \"object\", \"properties\": { \"leaf\": { \"type\": \"string\" } }, \"required\": [\"leaf\"] }"
                        : "{ \"type\": \"object\", \"properties\": { \"l" + (level + 1) + "\": { \"$ref\": \"#/$defs/D" + (level + 1)
                                + "\" } }, \"required\": [\"l" + (level + 1) + "\"] }";
                defs.append(level > 1 ? "," : "").append("\"D").append(level).append("\": ").append(body);
            }
            return "{ \"type\": \"object\", \"properties\": { \"l1\": { \"$ref\": \"#/$defs/D1\" } }, "
                    + "\"required\": [\"l1\"], \"$defs\": { " + defs + " } }";
        }

        /**
         * The structure of {@code node} with its scalar values erased, so two
         * documents can be compared on shape alone.
         */
        private static String shape(JsonNode node) {
            if (node.isObject()) {
                var fields = new ArrayList<String>();
                node.fields().forEachRemaining(entry -> fields.add(entry.getKey() + ":" + shape(entry.getValue())));
                return "{" + String.join(",", fields) + "}";
            }
            if (node.isArray()) {
                var elements = new ArrayList<String>();
                node.forEach(element -> elements.add(shape(element)));
                return "[" + String.join(",", elements) + "]";
            }
            return node.getNodeType().toString();
        }
    }

    /**
     * A {@code oneOf} or {@code anyOf} branch that is nothing but a
     * {@code $ref} constrains the value through the definition it names,
     * instead of leaving the parent schema to stand on its own.
     */
    @Nested
    class BranchReferences {

        private static final String SINGLE_REF_BRANCH = """
                {
                  "type": "object",
                  "%s": [
                    {
                      "$ref": "#/$defs/Named"
                    }
                  ],
                  "$defs": {
                    "Named": {
                      "type": "object",
                      "properties": {
                        "name": {
                          "type": "string",
                          "minLength": 1
                        }
                      },
                      "required": ["name"]
                    }
                  }
                }""";

        @ParameterizedTest
        @ValueSource(strings = {"oneOf", "anyOf"})
        void branchThatIsOnlyReferenceIsHonoured(String keyword) {
            // when
            var schema = SINGLE_REF_BRANCH.formatted(keyword);
            var json = Gjuton.of(schema).withSeed(1L).generate();
            var root = parse(json);

            // then
            assertThat(root.get("name").isTextual()).isTrue();
        }

        @Test
        void branchReferenceConflictingWithTheParentIsDroppedFromItsGroup() {
            // given a group where only the second branch can be an object
            var gen = Gjuton.of("""
                    {
                      "type": "object",
                      "oneOf": [
                        {
                          "$ref": "#/$defs/Text"
                        },
                        {
                          "$ref": "#/$defs/Named"
                        }
                      ],
                      "$defs": {
                        "Text": {
                          "type": "string"
                        },
                        "Named": {
                          "type": "object",
                          "properties": {
                            "name": {
                              "type": "string",
                              "minLength": 1
                            }
                          },
                          "required": ["name"]
                        }
                      }
                    }""").withSeed(1L);

            // when
            var generated = new ArrayList<JsonNode>();
            for (int i = 0; i < 5; i++) {
                generated.add(parse(gen.generate()));
            }

            // then
            assertThat(generated).allMatch(value -> value.get("name").isTextual());
        }

        @Test
        void groupWhoseEveryBranchReferenceConflictsWithTheParentFails() {
            // given
            var schema = """
                    {
                      "type": "object",
                      "oneOf": [
                        {
                          "$ref": "#/$defs/Text"
                        }
                      ],
                      "$defs": {
                        "Text": {
                          "type": "string"
                        }
                      }
                    }""";

            // when
            var thrown = catchThrowable(() -> Gjuton.of(schema).withSeed(1L).generate());

            // then
            assertThat(thrown)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("Unable to merge any oneOf branch with the parent schema");
        }

        @Test
        void mutuallyReferencingBranchesFailWithoutOverflowingTheStack() {
            // given two definitions whose only branch reaches the other, so no
            // property or element is ever generated between the expansions
            var gen = Gjuton.of("""
                    {
                      "$ref": "#/$defs/A",
                      "$defs": {
                        "A": {
                          "oneOf": [
                            {
                              "$ref": "#/$defs/B"
                            }
                          ]
                        },
                        "B": {
                          "oneOf": [
                            {
                              "$ref": "#/$defs/A"
                            }
                          ]
                        }
                      }
                    }""").withSeed(1L);

            // when
            var thrown = catchThrowable(gen::generate);

            // then
            assertThat(thrown)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("keeps recursing without producing a value");
        }
    }

    @Nested
    class UnsatisfiableReporting {

        @Test
        void namesOnlyTheConstraintsTheSchemaActuallyDeclares() {
            // given
            var gen = Gjuton.of("""
                    {"type": "string", "format": "uri", "pattern": "^https://fixed\\\\.example/only$"}
                    """);

            // when / then
            assertThatThrownBy(gen::generate)
                    .isInstanceOf(UnsatisfiableSchemaException.class)
                    .hasMessageContaining("pattern ^https://fixed\\.example/only$")
                    .hasMessageNotContaining("length");
        }

        @Test
        void locatesFailureAtTheDocumentRoot() {
            // given
            var gen = Gjuton.of("""
                    {"type": "string", "format": "uri", "pattern": "^https://fixed\\\\.example/only$"}
                    """);

            // when / then
            assertThatThrownBy(gen::generate).hasMessageEndingWith("(at $)");
        }
    }
}
