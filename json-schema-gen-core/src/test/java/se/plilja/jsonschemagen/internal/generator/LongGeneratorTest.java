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
    var generator = new LongGenerator(new Random(42), IntegerSchema.of(-10L, 10L));

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
    var generator = new LongGenerator(new Random(42), IntegerSchema.of(-10L, 10L));

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
    var generator = new LongGenerator(new Random(42), IntegerSchema.of(-5L, null));

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
  void maxOnlyCoversBoundaryValues() {
    var generator = new LongGenerator(new Random(42), IntegerSchema.of(null, 5L));

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
