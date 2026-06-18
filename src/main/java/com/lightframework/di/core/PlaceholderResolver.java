package com.lightframework.di.core;

@FunctionalInterface
public interface PlaceholderResolver {
    String resolvePlaceholder(String placeholder);
}
