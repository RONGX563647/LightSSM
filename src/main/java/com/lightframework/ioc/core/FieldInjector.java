package com.lightframework.ioc.core;

/**
 * 字段注入器函数式接口，优先使用 MethodHandle 提升性能
 */
@FunctionalInterface
public interface FieldInjector {
    void inject(Object bean, Object value) throws Exception;
}
