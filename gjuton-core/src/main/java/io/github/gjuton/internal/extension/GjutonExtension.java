package io.github.gjuton.internal.extension;

/**
 * A unit of functionality an artifact on the classpath contributes.
 *
 * <p>An extension declares itself as a {@link java.util.ServiceLoader}
 * service, so adding its artifact is all the configuration there is. What
 * it contributes is whatever it registers; nothing here says which
 * services those are.
 */
public interface GjutonExtension {

    /**
     * Names this extension, as a consumer would refer to it — for
     * instance when saying which of two to prefer.
     */
    String name();

    /**
     * Registers everything this extension provides. Called once, before
     * any service is looked up.
     */
    void init(ServiceRegistry registry);
}
