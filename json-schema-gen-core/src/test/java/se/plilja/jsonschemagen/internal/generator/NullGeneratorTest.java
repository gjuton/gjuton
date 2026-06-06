package se.plilja.jsonschemagen.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NullGeneratorTest {

    @Test
    void alwaysReturnsNull() {
        var generator = new NullGenerator();

        // when
        var values = IntStream.range(0, 20)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(values).containsOnly((Object) null);
    }
}
