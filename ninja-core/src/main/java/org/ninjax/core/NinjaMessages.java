package org.ninjax.core;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.ninjax.core.properties.NinjaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NinjaMessages {

    private static final Logger logger = LoggerFactory.getLogger(NinjaMessages.class);

    private static final String APPLICATION_LANGUAGES = "application.languages";
    private static final String BASE_NAME = "messages";
    private final Map<Locale, ResourceBundle> bundles;
    private final Locale defaultLocale;

    public NinjaMessages(NinjaProperties ninjaProperties) {

        List<Locale> supportedLocales = ninjaProperties.get(APPLICATION_LANGUAGES)
                .map(s -> Arrays.stream(s.split(","))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .map(l -> Locale.of(l))
                .toList()).orElseThrow(() -> new RuntimeException("Cannot find key " + APPLICATION_LANGUAGES + " in your application.conf. Please add a line similar to '" + APPLICATION_LANGUAGES + "=en,de'"));

        this.defaultLocale = supportedLocales.getFirst();

        var modifiableBundle = new HashMap<Locale, ResourceBundle>();

        // Load and cache ResourceBundles for all supported locales
        for (Locale locale : supportedLocales) {
            ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale);
            modifiableBundle.put(locale, bundle);
        }

        // Always ensure fallback is loaded
        if (!modifiableBundle.containsKey(defaultLocale)) {
            modifiableBundle.put(defaultLocale, ResourceBundle.getBundle(BASE_NAME, defaultLocale));
        }

        bundles = Map.copyOf(modifiableBundle);

    }

    public String getMessage(String key, Locale locale, Object... params) {
        // Try requested locale
        ResourceBundle bundle = bundles.getOrDefault(locale, bundles.get(defaultLocale));

        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            // Try to fallback to English if not already there
            if (locale != defaultLocale) {
                try {
                    pattern = bundles.get(defaultLocale).getString(key);
                } catch (MissingResourceException ex) {
                    logger.warn("Cannot find key in your translation messages file. That's something you should look into.", ex);
                    return key;
                }
            } else {
                logger.warn("Cannot find key in your translation messages file. That's something you should look into.");
                return key;
            }
        }
        return MessageFormat.format(pattern, params);
    }

}
