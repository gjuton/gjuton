package se.plilja.jsonschemagen.internal.generator;

final class FunctionalUtil {

  private FunctionalUtil() {
  }

  @SafeVarargs
  static <T> T coalesce(T... values) {
    for (T value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
