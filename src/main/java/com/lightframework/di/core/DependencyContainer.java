package com.lightframework.di.core;

import java.lang.reflect.Field;
import java.util.Map;

public interface DependencyContainer {
    <T> T getBean(String name, Class<T> requiredType) throws Exception;
    boolean containsBean(String name);
    Object createLazyProxy(Class<?> type, String beanName);
    Object resolveDependency(Class<?> type, String fieldName, String qualifier, boolean required) throws Exception;
    Object resolveDependencyWithGenerics(Field field, String qualifier, boolean required) throws Exception;
    <T> T getBean(Class<T> requiredType) throws Exception;
    <T> Map<String, T> getBeansOfType(Class<T> type) throws Exception;
}
