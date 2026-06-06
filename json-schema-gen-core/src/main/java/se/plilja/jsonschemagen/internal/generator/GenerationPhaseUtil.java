package se.plilja.jsonschemagen.internal.generator;

final class GenerationPhaseUtil {

  private GenerationPhaseUtil() {
  }

  static <E extends Enum<E>> E first(Class<E> enumClass) {
    return enumClass.getEnumConstants()[0];
  }

  static <E extends Enum<E>> E advanceToNext(E current) {
    E[] values = current.getDeclaringClass().getEnumConstants();
    int next = Math.min(current.ordinal() + 1, values.length - 1);
    return values[next];
  }
}
