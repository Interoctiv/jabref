package org.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseStandardStreams;

import org.slf4j.LoggerFactory;

/// Provides handling for messages and menu entries in the preferred language of the user.
///
/// Notes: All messages and menu-entries in JabRef are stored in escaped form like "This_is_a_message". This message
/// serves as key inside the `l10n` properties files that hold the translation for many languages. When a message
/// is accessed, it needs to be unescaped and possible parameters that can appear in a message need to be filled with
/// values.
///
/// This implementation loads the appropriate language by importing all keys/values from the correct bundle and stores
/// them in unescaped form inside a [LocalizationBundle] which provides fast access because it caches the key-value
/// pairs.
///
/// The access to this is given by the functions [#lang(String,String...)] and
/// that developers should use whenever they use strings for the e.g. GUI that need to be translatable.
@AllowedToUseStandardStreams("Needs to have acess to System.err because it's called very early before our loggers")
public class Localization {
    static final String RESOURCE_PREFIX = "l10n/JabRef";
    private static Locale locale;
    private static LocalizationBundle localizedMessages;

    private Localization() {
    }

    /**
     * Public access to all messages that are not menu-entries
     *
     * @param key    The key of the message in unescaped form like "All fields"
     * @param params Replacement strings for parameters %0, %1, etc.
     * @return The message with replaced parameters
     */
    public static String lang(String key, Object... params) {
        if (localizedMessages == null) {
            System.err.println("Messages are not initialized before accessing key: " + key);
            setLanguage(Language.ENGLISH);
        }
        var stringParams = Arrays.stream(params).map(Object::toString).toArray(String[]::new);
        return lookup(localizedMessages, key, stringParams);
    }

    /**
     * Sets the language and loads the appropriate translations. Note, that this function should be called before any
     * other function of this class.
     *
     * @param language Language identifier like "en", "de", etc.
     */
    public static void setLanguage(Language language) {
        Optional<Locale> knownLanguage = Language.convertToSupportedLocale(language);
        final Locale defaultLocale = Locale.getDefault();
        if (knownLanguage.isEmpty()) {
            LoggerFactory.getLogger(Localization.class).warn("Language {} is not supported by JabRef (Default: {})", language, defaultLocale);
            setLanguage(Language.ENGLISH);
            return;
        }
        // avoid reinitialization of the language bundles
        final Locale langLocale = knownLanguage.get();
        if ((locale != null) && locale.equals(langLocale) && locale.equals(defaultLocale)) {
            return;
        }
        locale = langLocale;
        Locale.setDefault(locale);

        try {
            createResourceBundles(locale);
        } catch (MissingResourceException ex) {
            // should not happen as we have scripts to enforce this
            LoggerFactory.getLogger(Localization.class).warn("Could not find bundles for language {}, switching to full english language", locale, ex);
            setLanguage(Language.ENGLISH);
        }
    }

    /**
     * Returns the messages bundle, e.g. to load FXML files correctly translated.
     *
     * @return The internally cashed bundle.
     */
    public static LocalizationBundle getMessages() {
        // avoid situations where this function is called before any language was set
        if (locale == null) {
            setLanguage(Language.ENGLISH);
        }
        return localizedMessages;
    }

    /**
     * Creates and caches the language bundles used in JabRef for a particular language. This function first loads
     * correct version of the "escaped" bundles that are given in {@link l10n}. After that, it stores the unescaped
     * version in a cached {@link LocalizationBundle} for fast access.
     *
     * @param locale Localization to use.
     */
    private static void createResourceBundles(Locale locale) {
        ResourceBundle messages = ResourceBundle.getBundle(RESOURCE_PREFIX, locale);
        Objects.requireNonNull(messages, "Could not load " + RESOURCE_PREFIX + " resource.");
        localizedMessages = new LocalizationBundle(createLookupMap(messages));
    }

    /**
     * Helper function to create a Map from the key/value pairs of a bundle.
     *
     * @param baseBundle JabRef language bundle with keys and values for translations.
     * @return Lookup map for the baseBundle.
     */
    private static Map<String, String> createLookupMap(ResourceBundle baseBundle) {
        final ArrayList<String> baseKeys = Collections.list(baseBundle.getKeys());
        return new HashMap<>(baseKeys.stream().collect(
                Collectors.toMap(
                        // not required to unescape content, because that is already done by the ResourceBundle itself
                        key -> key,
                        baseBundle::getString)
        ));
    }

    /**
     * This looks up a key in the bundle and replaces parameters %0, ..., %9 with the respective params given. Note that
     * the keys are the "unescaped" strings from the bundle property files.
     *
     * @param bundle The {@link LocalizationBundle} which is usually {@link Localization#localizedMessages}.
     * @param key    The lookup key.
     * @param params The parameters that should be inserted into the message
     * @return The final message with replaced parameters.
     */
    private static String lookup(LocalizationBundle bundle, String key, String... params) {
        Objects.requireNonNull(key);

        String translation = bundle.containsKey(key) ? bundle.getString(key) : "";
        if (translation.isEmpty()) {
            LoggerFactory.getLogger(Localization.class).warn("Warning: could not get translation for \"{}\" for locale {}", key, Locale.getDefault());
            translation = key;
        }
        return new LocalizationKeyParams(translation, params).replacePlaceholders();
    }

    /**
     * A bundle for caching localized strings. Needed to support JavaFX inline binding.
     */
    private static class LocalizationBundle extends ResourceBundle {

        private final Map<String, String> lookup;

        LocalizationBundle(Map<String, String> lookupMap) {
            lookup = lookupMap;
        }

        @Override
        public final Object handleGetObject(String key) {
            Objects.requireNonNull(key);
            return Optional.ofNullable(lookup.get(key))
                           .orElse(key);
        }

        @Override
        public Enumeration<String> getKeys() {
            return Collections.enumeration(lookup.keySet());
        }

        @Override
        protected Set<String> handleKeySet() {
            return lookup.keySet();
        }

        @Override
        public boolean containsKey(String key) {
            // Pretend we have every key
            return true;
        }
    }
}

