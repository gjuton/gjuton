package io.github.gjuton.internal.extension;

import java.util.Map;
import java.util.Optional;

/**
 * The services the extensions on this classpath provide.
 *
 * <p>Read-only, and fixed for the life of the JVM: what a classpath
 * offers is settled once every extension has been given the chance to
 * register.
 *
 * <p>An absent service is answered rather than thrown, so the caller
 * that knows which artifact supplies it can say so.
 */
public final class ServiceLocator {

    private final Map<Class<?>, Object> services;

    ServiceLocator(Map<Class<?>, Object> services) {
        this.services = Map.copyOf(services);
    }

    /**
     * The implementation of {@code service}, or empty when no extension
     * provides one.
     */
    public <T> Optional<T> find(Class<T> service) {
        var implementation = services.get(service);
        return Optional.ofNullable(service.cast(implementation));
    }
}
