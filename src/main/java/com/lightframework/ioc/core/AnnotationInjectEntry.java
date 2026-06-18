package com.lightframework.ioc.core;

/**
 * 注解注入条目：缓存类型和注入器（优先使用 MethodHandle 加速）
 */
public class AnnotationInjectEntry {
    public final Class<?> type;
    public final FieldInjector injector;

    public AnnotationInjectEntry(Class<?> type, FieldInjector injector) {
        this.type = type;
        this.injector = injector;
    }
}
