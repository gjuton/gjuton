package io.github.gjuton.internal.generator.format;

import static io.github.gjuton.internal.generator.TestContexts.withSeed;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.gjuton.errors.UnsatisfiableSchemaException;
import io.github.gjuton.internal.model.StringFormat;
import io.github.gjuton.internal.model.StringSchema;
import java.time.Duration;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class DurationGeneratorTest {

    // Day/time subset of ISO 8601 durations — the forms java.time.Duration.parse() accepts.
    private static final Pattern ISO_8601_DURATION = Pattern.compile(
            "^P(?=(?:\\d+[DHMS]|T\\d))(?:\\d+D)?(?:T(?:\\d+H)?(?:\\d+M)?(?:\\d+S)?)?$");

    private static void assertValidDuration(String value) {
        assertThat(value).matches(ISO_8601_DURATION);
        Duration.parse(value);
    }

    @Test
    void firstResultIsZeroDuration() {
        var schema = StringSchema.builder().format(StringFormat.DURATION).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        var first = generator.generate();

        // then
        assertThat(first).isEqualTo("P0D");
    }

    @Test
    void randomPhaseProducesValidIsoDurations() {
        var schema = StringSchema.builder().format(StringFormat.DURATION).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        generator.generate();
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(s -> assertValidDuration(s));
    }

    @Test
    void producesVariedValues() {
        var schema = StringSchema.builder().format(StringFormat.DURATION).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        var distinct = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .distinct()
                .count();

        // then
        assertThat(distinct).isGreaterThan(1);
    }

    @Test
    void unsatisfiableMaxLengthThrowsWithClearMessage() {
        // Shortest valid duration is "P0D" (3 chars); maxLength=2 cannot be satisfied.
        var schema = StringSchema.builder().format(StringFormat.DURATION).maxLength(2).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when / then
        assertThatThrownBy(generator::generate)
                .isInstanceOf(UnsatisfiableSchemaException.class)
                .hasMessageContaining("bounds exclude");
    }

    @Test
    void composesWithMaxLength() {
        // maxLength=4 admits durations like "P0D", "P3D", "P9Y" — all 3-4 chars.
        var schema = StringSchema.builder().format(StringFormat.DURATION).maxLength(4).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(s -> {
            assertValidDuration(s);
            assertThat(s.length()).isLessThanOrEqualTo(4);
        });
    }

    @Test
    void composesWithMinLength() {
        // minLength=6 excludes short forms like "P0D" (3 chars) — forces multi-component durations.
        var schema = StringSchema.builder().format(StringFormat.DURATION).minLength(6).build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(s -> {
            assertValidDuration(s);
            assertThat(s.length()).isGreaterThanOrEqualTo(6);
        });
    }

    @Test
    void composesWithPattern() {
        // Pattern restricts to durations containing a time part.
        var schema = StringSchema.builder().format(StringFormat.DURATION).pattern("T").build();
        var generator = new DurationGenerator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(s -> {
            assertValidDuration(s);
            assertThat(s).contains("T");
        });
    }
}
