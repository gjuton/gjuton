package se.plilja.jsonschemagen.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MathUtilTest {

    @ParameterizedTest
    @CsvSource({
            "7, 13, 1",
            "12, 18, 6",
            "0, 9, 9",
            "9, 0, 9",
            "-12, 18, 6",
            "12, -18, 6",
    })
    void gcd(long a, long b, long expected) {
        // when / then
        assertThat(MathUtil.gcd(a, b)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "3, 5, 15",
            "4, 12, 12",
            "0, 7, 0",
            "7, 0, 0",
    })
    void lcm(long a, long b, long expected) {
        // when / then
        assertThat(MathUtil.lcm(a, b)).isEqualTo(expected);
    }

    @Test
    void lcmOverflowThrows() {
        // when / then
        assertThatThrownBy(() -> MathUtil.lcm(Long.MAX_VALUE / 2, 7))
                .isInstanceOf(ArithmeticException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "5, 10, 0, 100, 5, 10",
            "0, 100, 5, 20, 5, 20",
            "5, 10, 0, 100, 5, 10",
            "-5, 50, 0, 100, 0, 50",
            "5, 200, 0, 100, 5, 100",
            "-5, 200, 0, 100, 0, 100",
            "50, 20, 0, 100, 50, 50",
            "10, 10, 0, 100, 10, 10",
    })
    void clampRange(int min, int max, int floor, int ceiling, int expectedMin, int expectedMax) {
        // when
        var range = MathUtil.clampRange(min, max, floor, ceiling);

        // then
        assertThat(range.min()).isEqualTo(expectedMin);
        assertThat(range.max()).isEqualTo(expectedMax);
    }

    @ParameterizedTest
    @CsvSource({
            "-50, -10, 0, 100",
            "200, 300, 0, 100",
    })
    void clampRangeRejectsNonOverlappingInput(int min, int max, int floor, int ceiling) {
        // when / then
        assertThatThrownBy(() -> MathUtil.clampRange(min, max, floor, ceiling))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void intRangeRejectsInvertedBounds() {
        // when / then
        assertThatThrownBy(() -> new MathUtil.IntRange(10, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
