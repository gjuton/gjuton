package io.github.gjuton.internal.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/**
 * Utilities for working with strings.
 */
public final class StringUtil {

    private StringUtil() {
    }

    public static String shortest(List<String> strings) {
        return strings.stream().min(Comparator.comparingInt(String::length)).orElseThrow();
    }

    public static String longest(List<String> strings) {
        return strings.stream().max(Comparator.comparingInt(String::length)).orElseThrow();
    }

    /**
     * The text {@code encoded} states, with its RFC 3986 percent-escapes read
     * as the characters they stand for. A {@code %} opening no valid escape
     * stands for itself, as does every other character — {@code +} included.
     */
    public static String decodePercentEscapes(String encoded) {
        var decoded = new StringBuilder(encoded.length());
        // A character outside US-ASCII is written as one escape per byte of its UTF-8
        // form, so escapes are gathered and read together rather than one at a time.
        var escapedBytes = new ByteArrayOutputStream();
        int index = 0;
        while (index < encoded.length()) {
            if (encoded.charAt(index) == '%' && index + 2 < encoded.length()) {
                int high = Character.digit(encoded.charAt(index + 1), 16);
                int low = Character.digit(encoded.charAt(index + 2), 16);
                if (high >= 0 && low >= 0) {
                    escapedBytes.write(high * 16 + low);
                    index += 3;
                    continue;
                }
            }
            if (escapedBytes.size() > 0) {
                decoded.append(escapedBytes.toString(StandardCharsets.UTF_8));
                escapedBytes.reset();
            }
            decoded.append(encoded.charAt(index));
            index++;
        }
        if (escapedBytes.size() > 0) {
            decoded.append(escapedBytes.toString(StandardCharsets.UTF_8));
        }
        return decoded.toString();
    }
}
