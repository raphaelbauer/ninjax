package org.r10r.ninjax.core;

import java.io.InputStream;
import java.util.logging.LogManager;

public final class NinjaJavaLogging {

    public static final String DEFAULT_LOGGING_PROPERTIES_LOCATION = "conf/logging.properties";

    private NinjaJavaLogging() {
    }

    /**
     * Initializes the default Java Logging and loads a logging.properties file.
     * 
     * A viable alternative is Logback. This can be configured via simple dependencies
     * in Gradle / Maven pom.xml.
     */
    public static void initialize() {
        initialize(DEFAULT_LOGGING_PROPERTIES_LOCATION);
    }

    public static void initialize(String loggingPropertesLocation) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(loggingPropertesLocation)) {
            if (inputStream != null) {
                LogManager.getLogManager().readConfiguration(inputStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logging configuration from " + loggingPropertesLocation + ": " + e.getMessage());
        }
    }

}
