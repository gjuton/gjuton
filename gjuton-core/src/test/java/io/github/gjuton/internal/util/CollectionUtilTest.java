package io.github.gjuton.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CollectionUtilTest {

    @Nested
    class Concat {

        @Test
        void concatenatesBothLists() {
            // when
            var result = CollectionUtil.concat(List.of("a", "b"), List.of("c", "d"));

            // then
            assertThat(result).containsExactly("a", "b", "c", "d");
        }

        @Test
        void returnsNullWhenBothNull() {
            // when
            var result = CollectionUtil.concat(null, null);

            // then
            assertThat(result).isNull();
        }

        @Test
        void returnsCopyOfSecondWhenFirstIsNull() {
            var second = new ArrayList<>(List.of("x", "y"));

            // when
            var result = CollectionUtil.concat(null, second);

            // then
            assertThat(result).containsExactly("x", "y");
            assertThat(result).isNotSameAs(second);
        }

        @Test
        void returnsCopyOfFirstWhenSecondIsNull() {
            var first = new ArrayList<>(List.of("x", "y"));

            // when
            var result = CollectionUtil.concat(first, null);

            // then
            assertThat(result).containsExactly("x", "y");
            assertThat(result).isNotSameAs(first);
        }

        @Test
        void doesNotModifyOriginalLists() {
            var first = new ArrayList<>(List.of("a"));
            var second = new ArrayList<>(List.of("b"));

            // when
            CollectionUtil.concat(first, second);

            // then
            assertThat(first).containsExactly("a");
            assertThat(second).containsExactly("b");
        }
    }
}
