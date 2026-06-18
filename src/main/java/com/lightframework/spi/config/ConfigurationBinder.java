package com.lightframework.spi.config;

import java.util.Map;

public interface ConfigurationBinder {
    <T> T bindProperties(Class<T> targetType);

    <T> T bindProperties(String prefix, Class<T> targetType);

    <T> T bindProperties(Map<String, String> properties, Class<T> targetType);
}
