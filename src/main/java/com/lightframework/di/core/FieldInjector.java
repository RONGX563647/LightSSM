package com.lightframework.di.core;

@FunctionalInterface
public interface FieldInjector {
    void inject(Object bean, Object value) throws Exception;
}
