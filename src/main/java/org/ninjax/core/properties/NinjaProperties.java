package org.ninjax.core.properties;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaProperties {

    private static Logger logger = LoggerFactory.getLogger(NinjaProperties.class);

    private final Properties properties;

    private static final String DEFAULT_LOCATION_OF_APPLICATION_CONF = "conf/application.conf";

    public NinjaProperties() {
        properties = loadProperties();
    }

    public Optional<String> get(String propertyName) {
        return Optional.ofNullable(properties.getProperty(propertyName));
    }

    public String getOrDie(String propertyName) {
        var value = properties.getProperty(propertyName);

        if (value == null) {
            throw new RuntimeException("Cannot find value in application.conf. Omg. Dying as requested. " + propertyName);
        }

        return value;
    }

    private Properties loadProperties() {

        Properties properties = new Properties();

        // Use the current thread's context class loader
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_LOCATION_OF_APPLICATION_CONF)) {
            if (inputStream == null) {
                logger.error("Sorry, unable to find " + DEFAULT_LOCATION_OF_APPLICATION_CONF);
                return properties;
            }

            // Load the properties file with UTF-8 encoding
            properties.load(new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8));
            logger.info("loaded!");

        } catch (IOException e) {
            logger.error("Opsi", e);
        }

        return properties;

    }
}
