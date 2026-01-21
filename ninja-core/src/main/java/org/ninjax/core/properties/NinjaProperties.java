package org.ninjax.core.properties;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaProperties {

    private static final Logger logger = LoggerFactory.getLogger(NinjaProperties.class);

    private final ImmutableMap<String, String> properties;

    public static final String DEFAULT_LOCATION_OF_APPLICATION_CONF = "conf/application.conf";

    public NinjaProperties() {
        properties = loadProperties();
    }

    public Optional<String> get(String propertyName) {
        return Optional.ofNullable(properties.get(propertyName));
    }

    public ImmutableMap<String, String> getAllProperties() {
        return this.properties;
    }

    private ImmutableMap loadProperties() {

        Properties properties = new Properties();

        ////////////////////////////////////////////////////////////////////////
        // Load Default properties
        ////////////////////////////////////////////////////////////////////////
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_LOCATION_OF_APPLICATION_CONF);
             InputStreamReader inputStreamReader = new java.io.InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            if (inputStream == null) {
                logger.error("Sorry, unable to find " + DEFAULT_LOCATION_OF_APPLICATION_CONF);
            } else {
                properties.load(inputStreamReader);
            }
        } catch (IOException e) {
            logger.error("Opsi. Failure loading " + DEFAULT_LOCATION_OF_APPLICATION_CONF, e);
        }

        ////////////////////////////////////////////////////////////////////////
        // Add all properties '-Dmy.property=...'
        // This overrides existing properties
        ////////////////////////////////////////////////////////////////////////
        properties.putAll(System.getProperties());

        return ImmutableMap.copyOf(properties);
    }
}
