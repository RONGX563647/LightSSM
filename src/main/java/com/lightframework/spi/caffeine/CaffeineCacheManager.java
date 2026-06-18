package com.lightframework.spi.caffeine;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.lightframework.spi.cache.Cache;
import com.lightframework.spi.cache.CacheManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CaffeineCacheManager implements CacheManager {

    private final Map<String, Cache<Object>> caches = new ConcurrentHashMap<>();
    private final Caffeine<Object, Object> caffeine;

    public CaffeineCacheManager() {
        this.caffeine = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats();
    }

    public CaffeineCacheManager(Caffeine<Object, Object> caffeine) {
        this.caffeine = caffeine;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Cache<V> getCache(String name) {
        return (Cache<V>) caches.computeIfAbsent(name, n ->
            new CaffeineCacheAdapter(caffeine.build()));
    }

    static class CaffeineCacheAdapter implements Cache<Object> {
        final com.github.benmanes.caffeine.cache.Cache<Object, Object> cache;

        CaffeineCacheAdapter(com.github.benmanes.caffeine.cache.Cache<Object, Object> cache) {
            this.cache = cache;
        }

        @Override
        public Object get(Object key) {
            return cache.getIfPresent(key);
        }

        @Override
        public void put(Object key, Object value) {
            cache.put(key, value);
        }

        @Override
        public void putIfAbsent(Object key, Object value) {
            cache.asMap().putIfAbsent(key, value);
        }

        @Override
        public void evict(Object key) {
            cache.invalidate(key);
        }

        @Override
        public void clear() {
            cache.invalidateAll();
        }

        @Override
        public Set<Object> keys() {
            return cache.asMap().keySet();
        }
    }
}
