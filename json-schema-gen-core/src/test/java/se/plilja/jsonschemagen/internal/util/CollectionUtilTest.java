package se.plilja.jsonschemagen.internal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CollectionUtilTest {

    @Nested
    class Reversed {
        @Test
        void reversedReturnsElementsInReverseOrder() {
            // when
            var result = CollectionUtil.reversed(List.of("a", "b", "c"));

            // then
            assertThat(result).containsExactly("c", "b", "a");
        }

        @Test
        void reversedReturnsEmptyListForEmptyInput() {
            // when
            var result = CollectionUtil.reversed(List.of());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void reversedReturnsSingleElementUnchanged() {
            // when
            var result = CollectionUtil.reversed(List.of("x"));

            // then
            assertThat(result).containsExactly("x");
        }

        @Test
        void reversedDoesNotModifyOriginalList() {
            var original = new java.util.ArrayList<>(List.of("a", "b", "c"));

            // when
            CollectionUtil.reversed(original);

            // then
            assertThat(original).containsExactly("a", "b", "c");
        }
    }
}
