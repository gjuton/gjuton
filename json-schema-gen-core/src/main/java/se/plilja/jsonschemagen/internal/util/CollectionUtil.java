package se.plilja.jsonschemagen.internal.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collection utilities.
 */
public final class CollectionUtil {

    private CollectionUtil() {
    }

    /**
     * Returns a new list with elements in reverse order.
     */
    public static <T> List<T> reversed(List<T> list) {
        var result = new ArrayList<>(list);
        Collections.reverse(result);
        return result;
    }
}
