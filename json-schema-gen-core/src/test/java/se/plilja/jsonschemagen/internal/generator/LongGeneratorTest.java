package se.plilja.jsonschemagen.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import se.plilja.jsonschemagen.internal.model.IntegerSchema;

class LongGeneratorTest {

    @Test
    void unconstrainedFirstCallProducesZero() {
        var generator = new LongGenerator(new Random(42), new IntegerSchema());

        // when
        long result = generator.generate();

        // then
        assertThat(result).isZero();
    }

    @Test
    void unconstrainedSubsequentCallsProduceVariedValues() {
        var generator = new LongGenerator(new Random(42), new IntegerSchema());
        generator.generate();

        // when
        var values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .collect(Collectors.toSet());

        // then
        assertThat(values).hasSizeGreaterThan(1);
    }

    @Test
    void boundedCoversBoundaryValues() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-10L, 10L, null, null, null));

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).contains(-10L, 10L, 0L, -9L, 9L);
    }

    @Test
    void boundedAllValuesWithinRange() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-10L, 10L, null, null, null));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= -10 && v <= 10);
    }

    @Test
    void minOnlyCoversBoundaryValues() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-5L, null, null, null, null));

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).contains(-5L, 0L, -4L);
        assertThat(values).allMatch(v -> v >= -5);
    }

    @Test
    void exclusiveMinimumExcludesBound() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, 10L, 5L, null, null));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v > 5 && v <= 10);
        assertThat(values).contains(6L, 10L);
    }

    @Test
    void exclusiveMaximumExcludesBound() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-10L, null, null, 5L, null));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= -10 && v < 5);
        assertThat(values).contains(-10L, 4L);
    }

    @Test
    void exclusiveBoundsOnlyCoversBoundaryValues() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, null, -10L, 10L, null));

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v > -10 && v < 10);
        assertThat(values).contains(-9L, 9L, 0L, -8L, 8L);
    }

    @Test
    void multipleOfAllValuesAreMultiples() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, null, null, null, 7L));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v % 7 == 0);
    }

    @Test
    void multipleOfWithBoundsCoversBoundaryMultiples() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-20L, 20L, null, null, 7L));

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= -20 && v <= 20 && v % 7 == 0);
        assertThat(values).contains(-14L, 14L, 0L);
    }

    @Test
    void multipleOfWithBoundsAllValuesValid() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(-20L, 20L, null, null, 7L));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= -20 && v <= 20 && v % 7 == 0);
    }

    @Test
    void multipleOfWithExclusiveBounds() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, null, -15L, 15L, 7L));

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v > -15 && v < 15 && v % 7 == 0);
        assertThat(values).contains(-14L, 14L, 0L);
    }

    @Test
    void maxOnlyCoversBoundaryValues() {
        var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, 5L, null, null, null));

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).contains(5L, 0L, 4L);
        assertThat(values).allMatch(v -> v <= 5);
    }
}
