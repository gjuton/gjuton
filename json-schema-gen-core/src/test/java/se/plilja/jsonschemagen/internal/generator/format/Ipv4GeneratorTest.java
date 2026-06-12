package se.plilja.jsonschemagen.internal.generator.format;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static se.plilja.jsonschemagen.internal.generator.TestContexts.withSeed;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import se.plilja.jsonschemagen.errors.UnsatisfiableSchemaException;
import se.plilja.jsonschemagen.internal.model.StringFormat;
import se.plilja.jsonschemagen.internal.model.StringSchema;

class Ipv4GeneratorTest {

    @Test
    void unsatisfiableMinLengthThrows() {
        // Longest IPv4 dotted-quad is 15 chars ("255.255.255.255").
        var schema = StringSchema.builder().format(StringFormat.IPV4).minLength(16).build();
        var generator = new Ipv4Generator(withSeed(42), schema);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void unsatisfiableMaxLengthThrows() {
        // Shortest IPv4 dotted-quad is 7 chars ("0.0.0.0").
        var schema = StringSchema.builder().format(StringFormat.IPV4).maxLength(6).build();
        var generator = new Ipv4Generator(withSeed(42), schema);

        // when / then
        assertThatThrownBy(generator::generate).isInstanceOf(UnsatisfiableSchemaException.class);
    }

    @Test
    void respectsLengthBounds() {
        // 7 = "0.0.0.0"; 11 narrows away the 12+ char addresses.
        var schema = StringSchema.builder().format(StringFormat.IPV4).minLength(7).maxLength(11).build();
        var generator = new Ipv4Generator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 50)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).allSatisfy(s -> assertThat(s.length()).isBetween(7, 11));
    }

    @Test
    void producesExpectedAddressesForFixedSeed() {
        var schema = StringSchema.builder().format(StringFormat.IPV4).build();
        var generator = new Ipv4Generator(withSeed(42), schema);

        // when
        var results = IntStream.range(0, 5)
                .mapToObj(i -> generator.generate())
                .toList();

        // then
        assertThat(results).containsExactly(
                "186.13.174.12",
                "79.241.70.181",
                "170.23.231.115",
                "94.97.70.176",
                "118.195.200.255");
    }
}
