package io.github.gjuton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gjuton.api.Constraints;
import io.github.gjuton.api.GenerationMode;
import io.github.gjuton.api.Gjuton;
import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.extension.GjutonExtensions;
import io.github.gjuton.internal.jsonconversion.JsonConverter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GjutonConstraintsTest {

    private static final JsonConverter JSON = GjutonExtensions.locator().find(JsonConverter.class).orElseThrow();
    private static final int SAMPLES = 100;

    @Test
    void numberRangeNarrowsUnboundedInteger() {
        // when
        var gen = Gjuton.of("""
                { "type": "integer" }""")
                .withConstraints(Constraints.of().numberRange(0, 10))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> assertThat(((Number) node).longValue()).isBetween(0L, 10L));
    }

    @Test
    void numberRangeIntersectsSchemaBounds() {
        // when
        var gen = Gjuton.of("""
                { "type": "integer", "minimum": 5, "maximum": 100 }""")
                .withConstraints(Constraints.of().numberRange(0, 10))
                .withSeed(1L);

        // then: schema floor of 5 still wins, constraint ceiling of 10 applies
        forEachValue(gen, node -> assertThat(((Number) node).longValue()).isBetween(5L, 10L));
    }

    @Test
    void numberRangeNarrowsNumber() {
        // when
        var gen = Gjuton.of("""
                { "type": "number" }""")
                .withConstraints(Constraints.of().numberRange(0.0, 1.0))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> assertThat(((Number) node).doubleValue()).isBetween(0.0, 1.0));
    }

    @Test
    void emptyNumberIntersectionIsUnsatisfiable() {
        // when
        var gen = Gjuton.of("""
                { "type": "integer", "minimum": 50, "maximum": 100 }""")
                .withConstraints(Constraints.of().numberRange(0, 10));

        // then
        assertThatThrownBy(gen::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void dateRangeNarrowsDate() {
        // when
        var min = Instant.parse("2000-01-01T00:00:00Z");
        var max = Instant.parse("2000-12-31T23:59:59Z");
        var gen = Gjuton.of("""
                { "type": "string", "format": "date" }""")
                .withConstraints(Constraints.of().dateRange(min, max))
                .withSeed(1L);

        // then
        var minDate = LocalDate.ofInstant(min, ZoneOffset.UTC);
        var maxDate = LocalDate.ofInstant(max, ZoneOffset.UTC);
        forEachValue(gen, node -> {
            var date = LocalDate.parse((String) node);
            assertThat(date).isBetween(minDate, maxDate);
        });
    }

    @Test
    void dateRangeNarrowsDateTime() {
        // when
        var min = Instant.parse("2000-01-01T00:00:00Z");
        var max = Instant.parse("2000-12-31T23:59:59Z");
        var gen = Gjuton.of("""
                { "type": "string", "format": "date-time" }""")
                .withConstraints(Constraints.of().dateRange(min, max))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> {
            var instant = OffsetDateTime.parse((String) node).toInstant();
            assertThat(instant).isBetween(min, max);
        });
    }

    @Test
    void stringLengthNarrowsLength() {
        // when
        var gen = Gjuton.of("""
                { "type": "string" }""")
                .withConstraints(Constraints.of().stringLength(2, 4))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> assertThat(((String) node).length()).isBetween(2, 4));
    }

    @Test
    void emptyStringLengthIntersectionIsUnsatisfiable() {
        // when
        var gen = Gjuton.of("""
                { "type": "string", "minLength": 3 }""")
                .withConstraints(Constraints.of().stringLength(0, 2));

        // then
        assertThatThrownBy(gen::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void alphabetRestrictsCharacters() {
        // when
        var gen = Gjuton.of("""
                { "type": "string" }""")
                .withConstraints(Constraints.of().alphabet("ABC").stringLength(5, 5))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> assertThat((String) node).matches("[ABC]{5}"));
    }

    @Test
    void arrayLengthNarrowsLength() {
        // when
        var gen = Gjuton.of("""
                { "type": "array", "items": { "type": "integer" } }""")
                .withConstraints(Constraints.of().arrayLength(2, 3))
                .withSeed(1L);

        // then
        forEachValue(gen, node -> assertThat(((List<?>) node).size()).isBetween(2, 3));
    }

    @Nested
    class ModeAwareDefaults {

        @Test
        void randomModeDatesFallWithinPreviousToNextYear() {
            int thisYear = Year.now(ZoneOffset.UTC).getValue();
            var gen = Gjuton.of("""
                    { "type": "string", "format": "date" }""")
                    .withSeed(1L);

            // when
            forEachValue(gen, node -> {
                var date = LocalDate.parse((String) node);
                assertThat(date.getYear()).isBetween(thisYear - 1, thisYear + 1);
            });
        }

        @Test
        void randomModeDateTimesFallWithinPreviousToNextYear() {
            int thisYear = Year.now(ZoneOffset.UTC).getValue();
            var gen = Gjuton.of("""
                    { "type": "string", "format": "date-time" }""")
                    .withSeed(1L);

            // when
            forEachValue(gen, node -> {
                var instant = OffsetDateTime.parse((String) node).toInstant();
                var year = instant.atOffset(ZoneOffset.UTC).getYear();
                assertThat(year).isBetween(thisYear - 1, thisYear + 1);
            });
        }

        @Test
        void exhaustiveModeDatesSpanWideRange() {
            var gen = Gjuton.of("""
                    { "type": "string", "format": "date" }""")
                    .withGenerationMode(GenerationMode.EXHAUSTIVE)
                    .withSeed(1L);

            // when
            boolean sawPre2000 = false;
            boolean sawPost2050 = false;
            for (int i = 0; i < SAMPLES; i++) {
                var node = parse(gen.generate());
                var date = LocalDate.parse((String) node);
                if (date.getYear() < 2000) {
                    sawPre2000 = true;
                }
                if (date.getYear() > 2050) {
                    sawPost2050 = true;
                }
            }

            // then
            assertThat(sawPre2000 || sawPost2050).isTrue();
        }

        @Test
        void randomModeIntegersFallWithinDefaultRange() {
            var gen = Gjuton.of("""
                    { "type": "integer" }""")
                    .withSeed(1L);

            // when / then
            forEachValue(gen, node ->
                    assertThat(((Number) node).longValue()).isBetween(-1_000_000L, 1_000_000L));
        }

        @Test
        void exhaustiveModeIntegersCanExceedRandomRange() {
            var gen = Gjuton.of("""
                    { "type": "integer" }""")
                    .withGenerationMode(GenerationMode.EXHAUSTIVE)
                    .withSeed(1L);

            // when
            boolean sawWide = false;
            for (int i = 0; i < SAMPLES; i++) {
                var node = parse(gen.generate());
                if (Math.abs(((Number) node).longValue()) > 1_000_000) {
                    sawWide = true;
                }
            }

            // then
            assertThat(sawWide).isTrue();
        }

        @Test
        void randomModeNumbersFallWithinDefaultRange() {
            var gen = Gjuton.of("""
                    { "type": "number" }""")
                    .withSeed(1L);

            // when / then
            forEachValue(gen, node ->
                    assertThat(((Number) node).doubleValue()).isBetween(-1_000_000.0, 1_000_000.0));
        }

        @Test
        void explicitConstraintsOverrideRandomModeDefaults() {
            var gen = Gjuton.of("""
                    { "type": "integer" }""")
                    .withConstraints(Constraints.of().numberRange(-10, 10))
                    .withSeed(1L);

            // when / then
            forEachValue(gen, node ->
                    assertThat(((Number) node).longValue()).isBetween(-10L, 10L));
        }
    }

    @Test
    void unsetKindsKeepSchemaBehaviour() {
        // when: only string length is constrained on an object with an unbounded integer
        var gen = Gjuton.of("""
                {
                  "type": "object",
                  "properties": { "s": { "type": "string" }, "n": { "type": "integer" } },
                  "required": ["s", "n"]
                }""")
                .withConstraints(Constraints.of().stringLength(3, 3))
                .withSeed(1L);

        // then: strings honour the length while integers range far past it
        boolean sawWideInteger = false;
        for (int i = 0; i < SAMPLES; i++) {
            var node = (Map<?, ?>) parse(gen.generate());
            assertThat((String) node.get("s")).hasSize(3);
            if (Math.abs(((Number) node.get("n")).longValue()) > 1000) {
                sawWideInteger = true;
            }
        }
        assertThat(sawWideInteger).isTrue();
    }

    private static void forEachValue(Gjuton gen, Consumer<Object> assertion) {
        IntStream.range(0, SAMPLES).forEach(i -> {
            var json = gen.generate();
            var node = parse(json);
            assertion.accept(node);
        });
    }

    private static Object parse(String json) {
        return JSON.readTree(json);
    }
}
