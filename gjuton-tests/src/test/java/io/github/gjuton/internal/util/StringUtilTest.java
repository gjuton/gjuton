package io.github.gjuton.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StringUtilTest {

    @Test
    void shortestReturnsShortest() {
        // when
        var result = StringUtil.shortest(List.of("ab", "a", "abc"));

        // then
        assertThat(result).isEqualTo("a");
    }

    @Test
    void shortestWithSingleElement() {
        // when
        var result = StringUtil.shortest(List.of("only"));

        // then
        assertThat(result).isEqualTo("only");
    }

    @Test
    void shortestWithEqualLengthsReturnsFirst() {
        // when
        var result = StringUtil.shortest(List.of("ab", "cd"));

        // then
        assertThat(result).isEqualTo("ab");
    }

    @Test
    void longestReturnsLongest() {
        // when
        var result = StringUtil.longest(List.of("ab", "a", "abc"));

        // then
        assertThat(result).isEqualTo("abc");
    }

    @Test
    void longestWithSingleElement() {
        // when
        var result = StringUtil.longest(List.of("only"));

        // then
        assertThat(result).isEqualTo("only");
    }

    @Test
    void longestWithEqualLengthsReturnsFirst() {
        // when
        var result = StringUtil.longest(List.of("ab", "cd"));

        // then
        assertThat(result).isEqualTo("ab");
    }

    @ParameterizedTest(name = "{0} decodes to {1}")
    @CsvSource({
            // An escape stands for the character it names.
            "a%3Ab, a:b",
            "100%25, 100%",
            // A character outside US-ASCII is one escape per byte of its UTF-8 form.
            "caf%C3%A9, café",
            "%E4%B8%AD, 中",
            // One written as itself rather than escaped is left as it is.
            "café, café",
            // An escape at the very end of the text.
            "a%3A, a:",
            // A "%" opening no valid escape stands for itself.
            "100%, 100%",
            "a%ZZb, a%ZZb",
            "a%4, a%4",
            // Even where a valid escape precedes it.
            "'a%20%wa', 'a %wa'",
            // "/" is not special here; what a decoded "/" separates is for the caller to say.
            "a%2Fb, a/b",
            // "+" is a literal, not a space.
            "a+b, a+b",
            // Text carrying no escape at all is unchanged.
            "plain, plain",
            "'', ''",
    })
    void decodePercentEscapesReadsEscapesAsTheCharactersTheyName(String encoded, String expected) {
        // when
        var result = StringUtil.decodePercentEscapes(encoded);

        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void decodePercentEscapesRejectsAnOverlongEncoding() {
        // "%C0%AF" is the overlong UTF-8 encoding of "/", the form used to smuggle a
        // separator past a decoder that reads each escaped byte on its own.
        // when
        var result = StringUtil.decodePercentEscapes("a%C0%AFb");

        // then
        assertThat(result).doesNotContain("/");
        assertThat(result).isEqualTo("a��b");
    }
}
