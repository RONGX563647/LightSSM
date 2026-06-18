package com.lightframework.spi.config;

import java.util.Map;

public interface PropertySource {
    String getProperty(String key);

    Map<String, String> getProperties(String prefix);

    boolean containsProperty(String key);
}
