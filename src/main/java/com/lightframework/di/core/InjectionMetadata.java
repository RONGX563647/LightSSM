package com.lightframework.di.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 注入元数据 — 预计算所有注入所需信息，注入热路径零反射。
 *
 * 极致优化设计：
 * 1. 所有注解信息在扫描阶段预计算，注入时不再调用 getAnnotation
 * 2. 使用 int[] 替代 Map<Object, Integer> 存储位索引，避免装箱
 * 3. 预绑定解析函数指针，消除枚举 switch 和虚方法调用
 * 4. 使用 Set 跟踪已注入字段，处理同一字段被多种注解标记的情况
 */
public class InjectionMetadata {
    public final List<AnnotationInjectEntry> autowiredEntries = new ArrayList<>();
    public final List<AnnotationInjectEntry> resourceEntries = new ArrayList<>();
    public final List<AnnotationInjectEntry> valueEntries = new ArrayList<>();
    public final List<Field> autowiredFields = new ArrayList<>();
    public final List<Field> resourceFields = new ArrayList<>();
    public final List<Field> valueFields = new ArrayList<>();
    public final List<Method> autowiredMethods = new ArrayList<>();
    public final List<Method> resourceMethods = new ArrayList<>();

    // ★ 同一字段/方法被多种注解标记时，记录已注入的，避免重复注入
    private final Set<Field> injectedFieldSet = new HashSet<>(8);
    private final Set<Method> injectedMethodSet = new HashSet<>(8);

    public byte awareFlags;

    /**
     * 检查字段是否已被注入（跨注解类型去重）
     */
    public boolean isFieldInjected(Field field) {
        return injectedFieldSet.contains(field);
    }

    /**
     * 标记字段已注入
     */
    public void markFieldInjected(Field field) {
        injectedFieldSet.add(field);
    }

    /**
     * 检查方法是否已被注入（跨注解类型去重）
     */
    public boolean isMethodInjected(Method method) {
        return injectedMethodSet.contains(method);
    }

    /**
     * 标记方法已注入
     */
    public void markMethodInjected(Method method) {
        injectedMethodSet.add(method);
    }
}
