package com.lightframework.spi.cache;

import java.util.Set;

public interface Cache<V> {
    V get(Object key);

    void put(Object key, V value);

    void putIfAbsent(Object key, V value);

    void evict(Object key);

    void clear();

    Set<Object> keys();

    default long size() { return keys().size(); }
}
