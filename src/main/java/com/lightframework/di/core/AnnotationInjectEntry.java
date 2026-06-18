package com.lightframework.di.core;

import java.lang.reflect.Field;

/**
 * 注解注入条目 — 极致优化版，预计算所有运行时所需信息。
 * 
 * ★ 预计算字段：
 * - type: 依赖类型
 * - injector: FieldInjector（MethodHandle 优先，反射回退）
 * - field: 原始 Field 引用（用于泛型类型解析）
 * - fieldName: 字段名（用于日志和错误信息，避免 field.getName() 调用）
 * - qualifier: @Qualifier 值（预计算，避免运行时 getAnnotation）
 * - required: 是否必需（预计算，避免运行时 getAnnotation）
 * - lazyProxy: @Lazy 代理对象（预创建，避免运行时 getAnnotation(Lazy.class)）
 * - resourceName: @Resource name 属性（预计算）
 * - placeholder: @Value 占位符值（预计算，避免运行时 getAnnotation(Value.class)）
 */
public class AnnotationInjectEntry {
    public final Class<?> type;
    public final FieldInjector injector;
    public final Field field;              // ★ 原始 Field，用于泛型类型解析
    public final String fieldName;         // ★ 预计算字段名
    public final String qualifier;         // ★ 预计算 @Qualifier
    public final boolean required;         // ★ 预计算 required
    public final Object lazyProxy;         // ★ 预计算 @Lazy 代理
    public final String resourceName;      // ★ 预计算 @Resource name
    public final String placeholder;       // ★ 预计算 @Value 占位符

    // ★ 构造函数：接收所有预计算信息
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector, Field field,
                                 String qualifier, boolean required, Object lazyProxy,
                                 String resourceName, String placeholder) {
        this.type = type;
        this.injector = injector;
        this.field = field;
        this.fieldName = field != null ? field.getName() : null;
        this.qualifier = qualifier;
        this.required = required;
        this.lazyProxy = lazyProxy;
        this.resourceName = resourceName;
        this.placeholder = placeholder;
    }

    // ★ 便捷构造函数（仅类型和注入器）
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector) {
        this(type, injector, null, null, true, null, null, null);
    }

    // ★ 便捷构造函数（带字段）
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector, Field field) {
        this(type, injector, field, null, true, null, null, null);
    }
}
