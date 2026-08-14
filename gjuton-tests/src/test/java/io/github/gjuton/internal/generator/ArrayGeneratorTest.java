package io.github.gjuton.internal.generator;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.extension.GjutonExtensions;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import io.github.gjuton.internal.model.ArraySchema;
import io.github.gjuton.internal.parser.SchemaParser;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ArrayGeneratorTest {

    private static final SchemaParser PARSER = new SchemaParser(GjutonExtensions.locator().find(JsonConverter.class).orElseThrow());

    @Test
    void minItemsIsAlwaysRespected() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).hasSizeGreaterThanOrEqualTo(3));
    }

    @Test
    void maxItemsIsAlwaysRespected() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 1,
                    "maxItems": 4
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).hasSizeLessThanOrEqualTo(4));
    }

    @Test
    void boundaryLengthsAreCoveredAcrossRepeatedCalls() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 2,
                    "maxItems": 5
                }
                """);

        // when
        var lengths = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .map(List::size)
                .toList();

        // then
        assertThat(lengths).contains(2);
        assertThat(lengths).contains(5);
    }

    @Test
    void emptyArrayIsCoveredWhenMinItemsIsZero() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "maxItems": 5
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).anyMatch(List::isEmpty);
    }

    @Test
    void elementsConformToItemsSubSchema() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "minItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).isNotEmpty());
        assertThat(results).allSatisfy(arr -> assertThat(arr).allMatch(e -> e instanceof String));
    }

    @Test
    void containsForcesNonEmptyArrayEvenWithoutMinItems() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "contains": {"const": "x"}
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).isNotEmpty();
            assertThat(arr).contains("x");
        });
    }

    @Test
    void containsRespectsMaxItems() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "contains": {"const": "x"},
                    "maxItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeLessThanOrEqualTo(2);
            assertThat(arr).contains("x");
        });
    }

    @Test
    void containsEnsuresMatchingElementIsPresent() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "contains": {"const": "required-value"},
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr ->
                assertThat(arr).contains("required-value"));
    }

    @Test
    void everyContainsClauseGetsMatchingElement() {
        var generator = mergedArrayGenerator("""
                {
                    "type": "array",
                    "allOf": [
                        {"contains": {"const": "A"}},
                        {"contains": {"const": "B"}}
                    ]
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(2);
            assertThat(arr).contains("A", "B");
        });
    }

    @Test
    void minContainsForcesSeveralMatchingElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "contains": {"const": 42},
                    "minContains": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(2);
            assertThat(arr).filteredOn(e -> e instanceof Number n && n.longValue() == 42).hasSizeGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void minContainsAppliesToEveryContainsClause() {
        var generator = mergedArrayGenerator("""
                {
                    "type": "array",
                    "allOf": [
                        {"contains": {"const": "A"}, "minContains": 2},
                        {"contains": {"const": "B"}, "minContains": 2}
                    ]
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).filteredOn("A"::equals).hasSizeGreaterThanOrEqualTo(2);
            assertThat(arr).filteredOn("B"::equals).hasSizeGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void relaxedMinContainsForcesNoMatchingElement() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "maxItems": 0,
                    "contains": {"const": "x"},
                    "minContains": 0
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).isEmpty());
    }

    @Test
    void minContainsAboveMaxItemsThrows() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "maxItems": 1,
                    "contains": {"const": 42},
                    "minContains": 2
                }
                """);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void maxContainsCapsTheMatchingElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"enum": ["x", "y", "z"]},
                    "contains": {"const": "x"},
                    "maxContains": 1,
                    "minItems": 2,
                    "maxItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).filteredOn("x"::equals).hasSize(1));
    }

    @Test
    void maxContainsNoElementCanRespectIsUnsatisfiable() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"const": "x"},
                    "contains": {"const": "x"},
                    "minItems": 3,
                    "maxContains": 1
                }
                """);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void severalRequiredMatchesLeaveThePrefixIntact() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [{"const": "first"}, {"const": "second"}],
                    "items": {"type": "integer"},
                    "contains": {"const": 42},
                    "minContains": 2,
                    "minItems": 4,
                    "maxItems": 4
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).startsWith("first", "second");
            assertThat(arr).filteredOn(e -> e instanceof Number n && n.longValue() == 42).hasSizeGreaterThanOrEqualTo(2);
        });
    }

    @Test
    void requiredMatchesMakeRoomPastThePrefix() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [{"const": "first"}],
                    "items": {"type": "integer"},
                    "contains": {"const": 42},
                    "minContains": 3,
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).startsWith("first");
            assertThat(arr).filteredOn(e -> e instanceof Number n && n.longValue() == 42).hasSizeGreaterThanOrEqualTo(3);
        });
    }

    @Test
    void containsBoundsWithoutContainsForceNoElement() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string"},
                    "maxItems": 0,
                    "minContains": 3
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).isEmpty());
    }

    @Test
    void draft7TupleProducesCorrectlyTypedPositionalElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": [
                        {"type": "string"},
                        {"type": "integer"}
                    ],
                    "minItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(2);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
        });
    }

    @Test
    void containsDoesNotOverrideTuplePositions() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": [
                        {"type": "string"},
                        {"type": "integer"}
                    ],
                    "contains": {"const": true},
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(3);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
            assertThat(arr).contains(true);
        });
    }

    @Test
    void prefixItemsProducesCorrectlyTypedPositionalElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [
                        {"type": "string"},
                        {"type": "integer"},
                        {"type": "boolean"}
                    ],
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(3);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
            assertThat(arr.get(2)).isInstanceOf(Boolean.class);
        });
    }

    @Test
    void additionalItemsFalseCapsArrayAtTupleSize() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": [
                        {"type": "string"},
                        {"type": "integer"}
                    ],
                    "additionalItems": false,
                    "minItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSize(2);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
        });
    }

    @Test
    void prefixItemsWithItemsFalseCapsArrayAtTupleSize() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [
                        {"type": "string"},
                        {"type": "integer"}
                    ],
                    "items": false,
                    "minItems": 2
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSize(2);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
        });
    }

    @Test
    void additionalItemsSchemaGeneratesTypedExtraElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": [
                        {"type": "string"}
                    ],
                    "additionalItems": {"type": "boolean"},
                    "minItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(3);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            for (int i = 1; i < arr.size(); i++) {
                assertThat(arr.get(i)).isInstanceOf(Boolean.class);
            }
        });
    }

    @Test
    void minItemsLargerThanTupleForcesGenerationPastPrefix() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [
                        {"type": "string"},
                        {"type": "integer"}
                    ],
                    "items": {"type": "boolean"},
                    "minItems": 4
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).hasSizeGreaterThanOrEqualTo(4);
            assertThat(arr.get(0)).isInstanceOf(String.class);
            assertThat(arr.get(1)).isInstanceOf(Number.class);
            for (int i = 2; i < arr.size(); i++) {
                assertThat(arr.get(i)).isInstanceOf(Boolean.class);
            }
        });
    }

    @Test
    void uniqueItemsProducesNoDuplicateElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer", "minimum": 0, "maximum": 1000},
                    "uniqueItems": true,
                    "minItems": 5,
                    "maxItems": 5
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).doesNotHaveDuplicates());
    }

    @Test
    void uniqueItemsWithSmallEnumStillProducesAllDistinctElements() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string", "enum": ["a", "b", "c"]},
                    "uniqueItems": true,
                    "minItems": 3,
                    "maxItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).doesNotHaveDuplicates();
            assertThat(arr).hasSize(3);
        });
    }

    @Test
    void uniqueItemsThrowsWhenMinItemsExceedsValueSpace() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "string", "enum": ["a", "b", "c"]},
                    "uniqueItems": true,
                    "minItems": 4
                }
                """);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void uniqueItemsAppliesAcrossPrefixAndAdditionalPositions() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [
                        {"type": "string", "enum": ["a", "b", "c"]},
                        {"type": "string", "enum": ["a", "b", "c"]}
                    ],
                    "items": {"type": "string", "enum": ["a", "b", "c"]},
                    "uniqueItems": true,
                    "minItems": 3,
                    "maxItems": 3
                }
                """);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> {
            assertThat(arr).doesNotHaveDuplicates();
            assertThat(arr).hasSize(3);
        });
    }

    @Test
    void uniqueItemsThrowsWhenPrefixItemsForceDuplicateConst() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "prefixItems": [
                        {"const": "x"},
                        {"const": "x"}
                    ],
                    "uniqueItems": true,
                    "minItems": 2
                }
                """);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void requiredPropertyMissingFromItemPropertiesIsGeneratedInEveryElement() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "id": {"type": "integer"}
                        },
                        "required": ["id", "label"]
                    },
                    "minItems": 2,
                    "maxItems": 4
                }
                """);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).allSatisfy(
                item -> assertThat((Map<String, Object>) item).containsKey("label")));
    }

    @Test
    void containsForcingMinLengthAboveMaxItemsThrows() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "minItems": 0,
                    "maxItems": 0,
                    "items": {"type": "integer"},
                    "contains": {"const": 42}
                }
                """);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void hugeMaxItemsIsCutToTheDefaultMaximum() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "maxItems": 2097152
                }
                """);

        // when
        var results = IntStream.range(0, 10)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(
                arr -> assertThat(arr).hasSizeLessThanOrEqualTo(1_000));
    }

    @Test
    void schemaWithoutMaxItemsStaysShort() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"}
                }
                """);

        // when
        var results = IntStream.range(0, 10)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).hasSizeLessThanOrEqualTo(5));
    }

    @Test
    void minItemsAboveTheCeilingIsGeneratedInFull() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "minItems": 2000,
                    "maxItems": 2097152
                }
                """);

        // when
        var results = IntStream.range(0, 10)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(arr -> assertThat(arr).hasSize(2000));
    }

    @Test
    void cuttingTheSchemaMaxItemsIsReportedOnce() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "maxItems": 2097152
                }
                """);

        // when
        try (var logs = LogCapture.of(ArrayGenerator.class)) {
            IntStream.range(0, 5).forEach(i -> generator.generate());

            // then
            assertThat(logs.messages())
                    .filteredOn(m -> m.contains("default maximum"))
                    .singleElement(as(STRING))
                    .contains("2097152", "1000");
        }
    }

    @Test
    void randomModeDoesNotReportCuttingTheSchemaMaxItems() {
        var document = PARSER.parse("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "maxItems": 2097152
                }
                """);
        var context = TestContexts.randomWithSeed(42);
        var generator = new ArrayGenerator(context, (ArraySchema) document.getRoot());

        // when
        try (var logs = LogCapture.of(ArrayGenerator.class)) {
            IntStream.range(0, 5).forEach(i -> generator.generate());

            // then
            assertThat(logs.messages()).noneMatch(m -> m.contains("default maximum"));
        }
    }

    @Test
    void minItemsAboveTheCeilingIsReportedOnce() {
        var generator = arrayGenerator("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "minItems": 2000
                }
                """);

        // when
        try (var logs = LogCapture.of(ArrayGenerator.class)) {
            IntStream.range(0, 5).forEach(i -> generator.generate());

            // then
            assertThat(logs.messages())
                    .filteredOn(m -> m.contains("default maximum"))
                    .singleElement(as(STRING))
                    .contains("2000", "1000");
        }
    }

    @Test
    void randomModeReportsMinItemsAboveTheCeiling() {
        var document = PARSER.parse("""
                {
                    "type": "array",
                    "items": {"type": "integer"},
                    "minItems": 200
                }
                """);
        var context = TestContexts.randomWithSeed(42);
        var generator = new ArrayGenerator(context, (ArraySchema) document.getRoot());

        // when
        try (var logs = LogCapture.of(ArrayGenerator.class)) {
            IntStream.range(0, 5).forEach(i -> generator.generate());

            // then
            assertThat(logs.messages())
                    .filteredOn(m -> m.contains("default maximum"))
                    .singleElement(as(STRING))
                    .contains("200");
        }
    }

    private static ArrayGenerator arrayGenerator(String json) {
        var document = PARSER.parse(json);
        return new ArrayGenerator(GeneratorContext.testContext(document, new Random(42)), (ArraySchema) document.getRoot());
    }

    /**
     * Builds a generator for the schema obtained by merging the {@code allOf}
     * branches of {@code json}. Needed for constraints such as several
     * {@code contains} clauses, which no single schema document can express.
     */
    private static ArrayGenerator mergedArrayGenerator(String json) {
        var document = PARSER.parse(json);
        var branches = document.getRoot().getAllOf();
        var merged = (ArraySchema) SchemaMerger.merge(branches);
        return new ArrayGenerator(GeneratorContext.testContext(document, new Random(42)), merged);
    }
}
