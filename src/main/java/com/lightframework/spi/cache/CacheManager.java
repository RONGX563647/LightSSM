package com.lightframework.spi.cache;

public interface CacheManager {
    <V> Cache<V> getCache(String name);
}
