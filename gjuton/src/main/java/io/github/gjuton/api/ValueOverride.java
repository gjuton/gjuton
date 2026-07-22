package io.github.gjuton.api;

/**
 * Supplies the value for a specific position in generated JSON, replacing
 * whatever the generator would otherwise produce there. Registered against a
 * path with {@link Gjuton#withOverrideByPath(String, ValueOverride)} or against
 * a property name with {@link Gjuton#withOverrideByName(String, ValueOverride)},
 * and invoked afresh on every {@link Gjuton#generate()} that
 * reaches a matching position, so it may return a different value each time.
 *
 * <p>The returned object may be anything that serialises to JSON — scalar,
 * {@code Collection}, {@code Map}, or bean. It is inserted as-is and is
 * <em>not</em> validated against the schema at its path.
 *
 * <p>Composes with a data-faker library in one line:
 * <pre>{@code
 * generator.withOverrideByPath("$.email", faker.internet()::emailAddress);
 * }</pre>
 */
@FunctionalInterface
public interface ValueOverride {

    /**
     * Returns the value to place at the registered position for the current
     * {@link Gjuton#generate()} call.
     */
    Object produce();
}
