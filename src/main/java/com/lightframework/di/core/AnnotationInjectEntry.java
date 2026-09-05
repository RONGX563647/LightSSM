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

    // TODO [L3][优化-建造者] AnnotationInjectEntry 有 8 个字段的构造函数，调用方极易把参数顺序传错。建议新增 AnnotationInjectEntryBuilder，用 withType/withInjector/withField/withQualifier/withLazyProxy 等链式方法构建，避免长参数列表导致的隐式 bug。；写对标志：按建造者模式完成实现，新增单测覆盖“分步构建+build()产出不可变对象”的主路径与一条必填项缺失的异常路径。
    // ★ 构造函数：接收所有预计算信息
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector, Field field,
                                 String qualifier, boolean required, Object lazyProxy,
                                 String resourceName, String placeholder) {
        this.type = type;
        this.injector = injector;
        this.field = field;
        // TODO [L2][练习] 手写 fieldName 的预计算推导——field 不为 null 时取 field.getName()，为 null 时置 null；理解「预计算」的意义：注入热路径不再调用 field.getName() 以减少反射开销。写对标志：构造后 entry.fieldName 与预期字段名一致。
        this.fieldName = field != null ? field.getName() : null;
        this.qualifier = qualifier;
        this.required = required;
        this.lazyProxy = lazyProxy;
        this.resourceName = resourceName;
        this.placeholder = placeholder;
    }

    // TODO [L1][练习] 手写便捷构造函数——仅类型+注入器（或再加字段）的构造函数应委托主构造函数，并把 qualifier 置 ""、required 置 true、lazyProxy/resourceName/placeholder 置 null；写对标志：用便捷构造出的 entry 其 fieldName 由 field 正确推导、其余可选字段为默认值。
    // ★ 便捷构造函数（仅类型和注入器）
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector) {
        this(type, injector, null, null, true, null, null, null);
    }

    // ★ 便捷构造函数（带字段）
    public AnnotationInjectEntry(Class<?> type, FieldInjector injector, Field field) {
        this(type, injector, field, null, true, null, null, null);
    }
}
