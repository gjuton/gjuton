package io.github.gjuton.internal.extension;

import java.util.Map;

/**
 * The services one extension contributes at start-up.
 *
 * <p>A service has a single provider: one another extension already
 * contributed is a conflict, not something to override.
 */
public final class ServiceRegistry {

    private final String extensionName;
    private final Map<Class<?>, Object> services;

    ServiceRegistry(String extensionName, Map<Class<?>, Object> services) {
        this.extensionName = extensionName;
        this.services = services;
    }

    /**
     * Claims {@code service} for {@code implementation}.
     *
     * @throws IllegalStateException if {@code service} is already claimed
     */
    public <T> void register(Class<T> service, T implementation) {
        var existing = services.putIfAbsent(service, implementation);
        if (existing != null) {
            throw new IllegalStateException(service.getName() + " is already provided by " + existing.getClass().getName() + "; extension '"
                    + extensionName + "' cannot provide it too. Keep one of them on the classpath.");
        }
    }
}
