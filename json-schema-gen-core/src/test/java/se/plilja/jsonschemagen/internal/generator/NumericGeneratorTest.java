package se.plilja.jsonschemagen.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static se.plilja.jsonschemagen.internal.generator.TestContexts.withSeed;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import se.plilja.jsonschemagen.internal.model.NumericSchema;

class NumericGeneratorTest {


    @Test
    void unconstrainedFirstCallProducesZero() {
        var generator = new NumericGenerator(withSeed(42),new NumericSchema());

        // when
        long result = generator.generate();

        // then
        assertThat(result).isZero();
    }

    @Test
    void unconstrainedSubsequentCallsProduceVariedValues() {
        var generator = new NumericGenerator(withSeed(42),new NumericSchema());
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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-10L).maximum(10L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-10L).maximum(10L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-5L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().maximum(10L).exclusiveMinimum(5L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-10L).exclusiveMaximum(5L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().exclusiveMinimum(-10L).exclusiveMaximum(10L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().multipleOf(7L).build());

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v % 7 == 0);
    }

    @Test
    void unboundedMultipleOfStaysWithinJsonSafeIntegerRange() {
        var safeMax = (1L << 53) - 1;
        var generator = new NumericGenerator(withSeed(20260607L), NumericSchema.builder().multipleOf(5L).build());

        // when
        List<Long> values = LongStream.range(0, 1000)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= -safeMax && v <= safeMax);
        assertThat(values).allMatch(v -> v % 5 == 0);
    }

    @Test
    void multipleOfWithBoundsCoversBoundaryMultiples() {
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-20L).maximum(20L).multipleOf(7L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().minimum(-20L).maximum(20L).multipleOf(7L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().exclusiveMinimum(-15L).exclusiveMaximum(15L).multipleOf(7L).build());

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
        var generator = new NumericGenerator(withSeed(42),NumericSchema.builder().maximum(5L).build());

        // when
        List<Long> values = LongStream.range(0, 20)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).contains(5L, 0L, 4L);
        assertThat(values).allMatch(v -> v <= 5);
    }

    @Test
    void minimumAndExclusiveMinimumTakeTighterBound() {
        // Both inclusive and exclusive lower bounds set — effective floor is max(0, 10+1) = 11.
        var generator = new NumericGenerator(withSeed(42),
                NumericSchema.builder().minimum(0L).exclusiveMinimum(10L).maximum(20L).build());

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v > 10 && v <= 20);
        assertThat(values).contains(11L, 20L);
    }

    @Test
    void maximumAndExclusiveMaximumTakeTighterBound() {
        // Both inclusive and exclusive upper bounds set — effective ceiling is min(20, 10-1) = 9.
        var generator = new NumericGenerator(withSeed(42),
                NumericSchema.builder().minimum(0L).maximum(20L).exclusiveMaximum(10L).build());

        // when
        List<Long> values = LongStream.range(0, 100)
                .map(i -> generator.generate())
                .boxed()
                .toList();

        // then
        assertThat(values).allMatch(v -> v >= 0 && v < 10);
        assertThat(values).contains(0L, 9L);
    }

    @Test
    void randomPhaseCoversEntireBoundedRange() {
        // The deterministic phases (MIN, MAX, NEAR_*) emit each boundary exactly once.
        // High counts (>50 in 1000 iterations) for both 0 and 3 prove the RANDOM phase
        // itself reaches the upper bound, not just the MAX phase.
        var generator = new NumericGenerator(withSeed(42),
                NumericSchema.builder().minimum(0L).maximum(3L).build());

        // when
        var counts = LongStream.range(0, 1000)
                .map(i -> generator.generate())
                .boxed()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        // then
        assertThat(counts.get(0L)).isGreaterThan(50L);
        assertThat(counts.get(3L)).isGreaterThan(50L);
    }

    @Test
    void randomPhaseCoversEntireMultipleOfRange() {
        // The deterministic phases (MIN, MAX, NEAR_*) emit each boundary multiple exactly
        // once. High counts (>50 in 1000 iterations) for both 0 and 20 prove the RANDOM
        // phase itself reaches the highest valid multiple, not just the MAX phase.
        var generator = new NumericGenerator(withSeed(42),
                NumericSchema.builder().minimum(0L).maximum(20L).multipleOf(5L).build());

        // when
        var counts = LongStream.range(0, 1000)
                .map(i -> generator.generate())
                .boxed()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()));

        // then
        assertThat(counts.get(0L)).isGreaterThan(50L);
        assertThat(counts.get(20L)).isGreaterThan(50L);
    }
}
