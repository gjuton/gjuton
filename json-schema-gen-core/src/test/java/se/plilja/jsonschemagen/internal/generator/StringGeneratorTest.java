package se.plilja.jsonschemagen.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;
import se.plilja.jsonschemagen.internal.model.StringSchema;

class StringGeneratorTest {

    @Test
    void firstCallProducesEmptyString() {
        var generator = new StringGenerator(new Random(42), new StringSchema());

        // when
        String result = generator.generate();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void subsequentCallsProduceNonEmptyStrings() {
        var generator = new StringGenerator(new Random(42), new StringSchema());
        generator.generate();

        // when
        String second = generator.generate();
        String third = generator.generate();

        // then
        assertThat(second).isNotEmpty();
        assertThat(third).isNotEmpty();
    }
}
