package com.lightframework.ioc.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注解元数据：存储一个类层次结构中的所有注入注解信息
 * 从 DefaultListableBeanFactory 中提取，供注入引擎使用
 */
public class AnnotationMetadata {
    final List<AnnotationInjectEntry> autowiredEntries = new ArrayList<>();
    final List<AnnotationInjectEntry> resourceEntries = new ArrayList<>();
    final List<AnnotationInjectEntry> valueEntries = new ArrayList<>();
    final List<Field> autowiredFields = new ArrayList<>();
    final List<Field> resourceFields = new ArrayList<>();
    final List<Field> valueFields = new ArrayList<>();
    final List<Method> autowiredMethods = new ArrayList<>();
    final List<Method> resourceMethods = new ArrayList<>();

    // Aware 类型位掩码 — bit 0: BeanNameAware, bit 1: BeanFactoryAware, bit 2: ApplicationContextAware
    byte awareFlags;

    // Field/Method → 位位置，用于 BitSet 防止重复注入
    final Map<Object, Integer> fieldIndex = new HashMap<>(8);

    void buildFieldIndex() {
        int idx = 0;
        for (Field f : autowiredFields) fieldIndex.put(f, idx++);
        for (Field f : resourceFields) fieldIndex.put(f, idx++);
        for (Field f : valueFields) fieldIndex.put(f, idx++);
        for (Method m : autowiredMethods) fieldIndex.put(m, idx++);
        for (Method m : resourceMethods) fieldIndex.put(m, idx++);
    }
}
