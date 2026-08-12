package io.github.gjuton.internal.generator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.slf4j.LoggerFactory;

/**
 * Collects what a class logs while a test exercises it, so a test can assert
 * on messages meant for the user. Capturing starts on creation and stops on
 * {@link #close()}, restoring the logger to its configured state; use it as a
 * try-with-resources.
 */
public final class LogCapture implements AutoCloseable {

    private final Logger logger;
    private final ListAppender<ILoggingEvent> appender;
    private final Level configuredLevel;

    private LogCapture(Logger logger, ListAppender<ILoggingEvent> appender, Level configuredLevel) {
        this.logger = logger;
        this.appender = appender;
        this.configuredLevel = configuredLevel;
    }

    /**
     * Captures everything {@code loggingClass} logs from now on, at every level.
     */
    public static LogCapture of(Class<?> loggingClass) {
        var logger = (Logger) LoggerFactory.getLogger(loggingClass);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        var capture = new LogCapture(logger, appender, logger.getLevel());
        logger.setLevel(Level.TRACE);
        logger.addAppender(appender);
        return capture;
    }

    /**
     * The messages logged so far, oldest first, with their placeholders filled in.
     */
    public List<String> messages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(configuredLevel);
        appender.stop();
    }
}
