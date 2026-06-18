package com.lightframework.ioc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean factory post processor that resolves ${...} placeholders in bean definitions
 * by loading properties from specified resource locations.
 *
 * <p>Supports:</p>
 * <ul>
 *   <li>Loading properties from classpath resources</li>
 *   <li>System property fallback when a key is not found in loaded properties</li>
 *   <li>Default values in placeholder syntax: ${key:defaultValue}</li>
 * </ul>
 *
 * <p>Compatible with Spring's PropertyPlaceholderConfigurer.</p>
 */
public class PropertyPlaceholderConfigurer implements BeanFactoryPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(PropertyPlaceholderConfigurer.class);

    /**
     * Prefix for placeholder syntax.
     */
    public static final String PLACEHOLDER_PREFIX = "${";

    /**
     * Suffix for placeholder syntax.
     */
    public static final String PLACEHOLDER_SUFFIX = "}";

    /**
     * Separator for default value in placeholder.
     */
    public static final String VALUE_SEPARATOR = ":";

    private String[] locations = new String[0];

    /**
     * Property values loaded from locations (thread-safe).
     */
    private final Map<String, String> propertyMap = new ConcurrentHashMap<>();

    /**
     * Set the locations of properties files to load.
     *
     * @param locations array of classpath resource locations (e.g., "app.properties")
     */
    public void setLocations(String[] locations) {
        this.locations = locations != null ? locations.clone() : new String[0];
    }

    /**
     * Add a location of a properties file to load.
     *
     * @param location classpath resource location (e.g., "app.properties")
     */
    public void addLocation(String location) {
        String[] newLocations = new String[this.locations.length + 1];
        System.arraycopy(this.locations, 0, newLocations, 0, this.locations.length);
        newLocations[this.locations.length] = location;
        this.locations = newLocations;
    }

    /**
     * Get the loaded property map.
     *
     * @return unmodifiable map of property key-value pairs
     */
    public Map<String, String> getPropertyMap() {
        return new HashMap<>(propertyMap);
    }

    /**
     * Get a property value by key. Falls back to system properties if not found.
     *
     * @param key the property key
     * @return the property value, or null if not found
     */
    public String getProperty(String key) {
        if (key == null) {
            return null;
        }
        String value = propertyMap.get(key);
        if (value == null) {
            // Fallback to system properties
            value = System.getProperty(key);
        }
        if (value == null) {
            // Fallback to environment variables
            value = System.getenv(key);
        }
        return value;
    }

    /**
     * 占位符解析递归深度限制，防止循环引用导致 StackOverflow
     */
    private static final int MAX_DEPTH = 64;
    private static final ThreadLocal<Integer> RESOLUTION_DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * Resolve placeholders in the given value string.
     *
     * <p>Supports:</p>
     * <ul>
     *   <li>${key} - resolved from properties or system properties</li>
     *   <li>${key:default} - resolved from properties, falls back to default value</li>
     * </ul>
     *
     * @param value the value string containing placeholders
     * @return the resolved value with placeholders replaced
     * @throws IllegalArgumentException if a placeholder cannot be resolved and has no default value
     *         or if recursion depth exceeds the maximum limit (prevents circular references)
     */
    public String resolvePlaceholder(String value) {
        return resolvePlaceholder(value, 0);
    }

    private String resolvePlaceholder(String value, int depth) {
        if (value == null || !value.contains(PLACEHOLDER_PREFIX)) {
            return value;
        }
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                "Placeholder resolution depth exceeded maximum (" + MAX_DEPTH + "), possible circular reference in: " + value);
        }

        StringBuilder result = new StringBuilder();
        int currentIndex = 0;

        while (currentIndex < value.length()) {
            int startIndex = value.indexOf(PLACEHOLDER_PREFIX, currentIndex);
            if (startIndex == -1) {
                result.append(value.substring(currentIndex));
                break;
            }

            // Append text before placeholder
            result.append(value.substring(currentIndex, startIndex));

            int endIndex = value.indexOf(PLACEHOLDER_SUFFIX, startIndex + PLACEHOLDER_PREFIX.length());
            if (endIndex == -1) {
                // Not a valid placeholder, treat as literal
                result.append(PLACEHOLDER_PREFIX);
                currentIndex = startIndex + PLACEHOLDER_PREFIX.length();
                continue;
            }

            // Extract placeholder content
            String placeholderContent = value.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
            String resolvedValue = resolvePlaceholderKey(placeholderContent, depth + 1);

            result.append(resolvedValue);
            currentIndex = endIndex + PLACEHOLDER_SUFFIX.length();
        }

        return result.toString();
    }

    /**
     * Resolve a single placeholder key, optionally with a default value.
     *
     * @param placeholderContent the placeholder content (e.g., "key" or "key:default")
     * @param depth current recursion depth (for cycle detection)
     * @return the resolved value
     * @throws IllegalArgumentException if key not found and no default provided
     */
    private String resolvePlaceholderKey(String placeholderContent, int depth) {
        String key;
        String defaultValue = null;

        // Check for default value separator
        int separatorIndex = placeholderContent.indexOf(VALUE_SEPARATOR);
        if (separatorIndex != -1) {
            key = placeholderContent.substring(0, separatorIndex).trim();
            defaultValue = placeholderContent.substring(separatorIndex + VALUE_SEPARATOR.length());
            // 递归解析默认值中的嵌套占位符
            defaultValue = resolvePlaceholder(defaultValue, depth + 1);
        } else {
            key = placeholderContent.trim();
        }

        String resolvedValue = getProperty(key);

        if (resolvedValue != null) {
            // 递归解析值中的嵌套占位符
            return resolvePlaceholder(resolvedValue, depth + 1);
        }

        if (defaultValue != null) {
            logger.debug("Property '{}' not found, using default value: {}", key, defaultValue);
            return defaultValue;
        }

        throw new IllegalArgumentException("Could not resolve placeholder '" + key + "'");
    }

    /**
     * Load properties from the specified locations.
     */
    private void loadProperties() {
        for (String location : locations) {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(location)) {
                if (is == null) {
                    logger.warn("Could not find properties file: {}", location);
                    continue;
                }

                Properties props = new Properties();
                props.load(is);

                for (String name : props.stringPropertyNames()) {
                    propertyMap.put(name, props.getProperty(name));
                }

                logger.info("Loaded {} properties from: {}", props.size(), location);
            } catch (IOException e) {
                logger.error("Failed to load properties from: {}", location, e);
            }
        }
    }

    @Override
    public void postProcessBeanFactory(DefaultListableBeanFactory beanFactory) throws Exception {
        loadProperties();
        beanFactory.setPropertyPlaceholderConfigurer(this);
        logger.info("PropertyPlaceholderConfigurer initialized with {} properties", propertyMap.size());
    }
}
