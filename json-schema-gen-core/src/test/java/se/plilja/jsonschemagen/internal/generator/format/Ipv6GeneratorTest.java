package se.plilja.jsonschemagen.internal.generator.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.plilja.jsonschemagen.internal.generator.TestContexts.withSeed;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.model.StringFormat;
import se.plilja.jsonschemagen.internal.model.StringSchema;

class Ipv6GeneratorTest {

    @Test
    void producesExpectedAddressesForFixedSeed() {
        var schema = StringSchema.builder().format(StringFormat.IPV6).build();
        var generator = new Ipv6Generator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 5)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).containsExactly(
                "ba41:dfe:aee7:c45:4f08:f12b:46ee:b52c",
                "aa61:1761:e743:7385:5e68:61b3:4697:b0bf",
                "76b2:c318:c86c:ff88:eb59:26e6:6fbd:7096",
                "bff9:ee3d:62f6:cc5c:2d68:268a:9827:569b",
                "35b3:402b:d372:5d7b:2c16:289d:9661:4630");
    }

    @Test
    void unsatisfiableMaxLengthThrows() {
        // Shortest uncompressed form is "0:0:0:0:0:0:0:0" (15 chars).
        var schema = StringSchema.builder().format(StringFormat.IPV6).maxLength(14).build();
        var generator = new Ipv6Generator(withSeed(42), schema);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void unsatisfiableMinLengthThrows() {
        // Longest uncompressed form is "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff" (39 chars).
        var schema = StringSchema.builder().format(StringFormat.IPV6).minLength(40).build();
        var generator = new Ipv6Generator(withSeed(42), schema);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }
}
