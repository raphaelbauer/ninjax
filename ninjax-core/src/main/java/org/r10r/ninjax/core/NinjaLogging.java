package org.r10r.ninjax.core;

import java.io.InputStream;
import java.util.logging.LogManager;

public final class NinjaLogging {

    public static final String DEFAULT_LOGGING_PROPERTIES_LOCATION = "conf/logging.properties";

    private NinjaLogging() {
    }

    public static void initialize() {
        initialize(DEFAULT_LOGGING_PROPERTIES_LOCATION);
    }

    public static void initialize(String propertiesLocation) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(propertiesLocation)) {
            if (inputStream != null) {
                LogManager.getLogManager().readConfiguration(inputStream);
            }
        } catch (Exception e) {
            System.err.println("Failed to load logging configuration from " + propertiesLocation + ": " + e.getMessage());
        }
    }

//    public static void installJulToSlf4jBridge() {
//        try {
//            Class<?> bridgeHandlerClass = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
//            Class<?> bridgeInstallerClass = Class.forName("org.slf4j.bridge.SLF4JBridgeHandler");
//            java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
//            java.util.logging.Handler[] handlers = rootLogger.getHandlers();
//            for (java.util.logging.Handler handler : handlers) {
//                rootLogger.removeHandler(handler);
//            }
//            bridgeInstallerClass.getMethod("install").invoke(null);
//        } catch (Exception e) {
//            System.err.println("jul-to-slf4j bridge not available: " + e.getMessage());
//        }
//    }
}
