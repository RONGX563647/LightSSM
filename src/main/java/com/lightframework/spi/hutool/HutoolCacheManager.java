package com.lightframework.spi.hutool;

import com.lightframework.spi.cache.Cache;
import com.lightframework.spi.cache.CacheManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HutoolCacheManager implements CacheManager {

    private final Map<String, Cache<Object>> caches = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <V> Cache<V> getCache(String name) {
        return (Cache<V>) caches.computeIfAbsent(name, n -> new HutoolCacheAdapter());
    }

    static class HutoolCacheAdapter implements Cache<Object> {
        private final Map<Object, Object> store = new ConcurrentHashMap<>();

        @Override
        public Object get(Object key) {
            return store.get(key);
        }

        @Override
        public void put(Object key, Object value) {
            store.put(key, value);
        }

        @Override
        public void putIfAbsent(Object key, Object value) {
            store.putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            store.remove(key);
        }

        @Override
        public void clear() {
            store.clear();
        }

        @Override
        public Set<Object> keys() {
            return new HashSet<>(store.keySet());
        }
    }
}
