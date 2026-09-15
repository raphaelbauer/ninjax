package org.r10r.ninjax.core.properties;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration of the application. Values are looked up in this order (first one wins):
 * <ol>
 * <li>Java system properties, e.g. {@code -Dapplication.secret=...}</li>
 * <li>Environment variables, named like the property in upper case with "." replaced by "_",
 * e.g. {@code APPLICATION_SECRET} for {@code application.secret}</li>
 * <li>{@code conf/application.conf} on the classpath</li>
 * </ol>
 * Environment variables keep secrets out of files that end up in version control.
 */
public class NinjaProperties {

    private static final Logger logger = Logger.getLogger(NinjaProperties.class.getName());

    private final Map<String, String> properties;
    private final Map<String, String> environmentVariables;

    public static final String DEFAULT_LOCATION_OF_APPLICATION_CONF = "conf/application.conf";

    public NinjaProperties() {
        this(System.getenv());
    }

    /**
     * @param environmentVariables usually {@link System#getenv()}. Tests can pass their own map.
     */
    public NinjaProperties(Map<String, String> environmentVariables) {
        this.environmentVariables = Map.copyOf(environmentVariables);
        this.properties = loadProperties();
    }

    public Optional<String> get(String propertyName) {
        if (properties.containsKey(propertyName)) {
            return Optional.of(properties.get(propertyName));
        }
        // Not in application.conf and not a system property. It can still be an environment variable.
        return Optional.ofNullable(environmentVariables.get(toEnvironmentVariableName(propertyName)));
    }

    /**
     * All properties from application.conf (with overrides applied) and all system properties.
     * Environment variables only show up here if they override a key from application.conf,
     * because an environment variable name can't be turned back into a property name reliably.
     */
    public Map<String, String> getAllProperties() {
        return this.properties;
    }

    /**
     * "application.session.expire_time_in_seconds" becomes "APPLICATION_SESSION_EXPIRE_TIME_IN_SECONDS".
     */
    public static String toEnvironmentVariableName(String propertyName) {
        return propertyName.replace('.', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private Map<String, String> loadProperties() {

        Properties properties = new Properties();

        ////////////////////////////////////////////////////////////////////////
        // Load Default properties
        ////////////////////////////////////////////////////////////////////////
        // Only create the reader once we know the file exists. Creating it in the try-with-resources
        // header threw a NullPointerException before the null check could log a helpful message.
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(DEFAULT_LOCATION_OF_APPLICATION_CONF)) {
            if (inputStream == null) {
                logger.log(Level.SEVERE, "Sorry, unable to find " + DEFAULT_LOCATION_OF_APPLICATION_CONF);
            } else {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Opsi. Failure loading " + DEFAULT_LOCATION_OF_APPLICATION_CONF, e);
        }

        ////////////////////////////////////////////////////////////////////////
        // Environment variables override keys of application.conf,
        // e.g. APPLICATION_SECRET overrides application.secret
        ////////////////////////////////////////////////////////////////////////
        for (String key : properties.stringPropertyNames()) {
            String environmentValue = environmentVariables.get(toEnvironmentVariableName(key));
            if (environmentValue != null) {
                properties.put(key, environmentValue);
            }
        }

        ////////////////////////////////////////////////////////////////////////
        // Add all properties '-Dmy.property=...'
        // This overrides existing properties
        ////////////////////////////////////////////////////////////////////////
        System.getProperties().forEach((key, value) ->
            properties.put(String.valueOf(key), String.valueOf(value))
        );

        // Convert Properties to Map<String, String>
        Map<String, String> stringMap = new HashMap<>();
        properties.forEach((key, value) ->
            stringMap.put(String.valueOf(key), String.valueOf(value))
        );

        return Map.copyOf(stringMap);
    }
}
