package io.github.gjuton.internal.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class GjutonMdcTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void clearsEveryKeyItDeclares() throws IllegalAccessException {
        // given
        var keys = new ArrayList<String>();
        for (var field : GjutonMdc.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                var key = (String) field.get(null);
                keys.add(key);
                MDC.put(key, "set");
            }
        }
        MDC.put("someoneElse", "set");

        // when
        GjutonMdc.clear();

        // then
        assertThat(keys).isNotEmpty();
        assertThat(keys).allSatisfy(key -> assertThat(MDC.get(key)).isNull());
        assertThat(MDC.get("someoneElse")).isEqualTo("set");
    }
}
