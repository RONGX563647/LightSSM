package com.lightframework.di;

import com.lightframework.di.core.PlaceholderResolver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderResolverTest {

    // 验证 TODO[L1] 练习目标：手写支持 ${key:default} 语法的解析器
    static class MapPlaceholderResolver implements PlaceholderResolver {
        private final Map<String, String> props;
        MapPlaceholderResolver(Map<String, String> props) { this.props = props; }

        @Override
        public String resolvePlaceholder(String placeholder) {
            if (placeholder == null) return null;
            String key = placeholder;
            String def = null;
            int colon = placeholder.indexOf(':');
            if (colon >= 0) {
                key = placeholder.substring(0, colon);
                def = placeholder.substring(colon + 1);
            }
            return props.getOrDefault(key, def);
        }
    }

    @Test
    void resolvesExistingKey() {
        PlaceholderResolver r = new MapPlaceholderResolver(Map.of("app.name", "light"));
        assertEquals("light", r.resolvePlaceholder("app.name"));
    }

    @Test
    void resolvesDefaultWhenMissing() {
        PlaceholderResolver r = new MapPlaceholderResolver(Map.of());
        assertEquals("def", r.resolvePlaceholder("miss:def"));
    }

    @Test
    void returnsNullWhenNoDefaultAndMissing() {
        PlaceholderResolver r = new MapPlaceholderResolver(Map.of());
        assertNull(r.resolvePlaceholder("gone"));
    }
}
