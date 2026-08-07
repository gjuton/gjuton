package io.github.gjuton.internal.extension;

import java.util.HashMap;
import java.util.ServiceLoader;

/**
 * The extensions this classpath provides, started up once per JVM.
 *
 * <p>A classpath cannot change while it runs, so what the extensions
 * offer is resolved on first use and holds for the life of the JVM.
 */
public final class GjutonExtensions {

    private static final ServiceLocator LOCATOR = startUp();

    private GjutonExtensions() {
    }

    /**
     * The services every extension on this classpath registered.
     */
    public static ServiceLocator locator() {
        return LOCATOR;
    }

    private static ServiceLocator startUp() {
        var services = new HashMap<Class<?>, Object>();
        var extensions = ServiceLoader.load(GjutonExtension.class);
        for (var extension : extensions) {
            var registry = new ServiceRegistry(extension.name(), services);
            extension.init(registry);
        }
        return new ServiceLocator(services);
    }
}
