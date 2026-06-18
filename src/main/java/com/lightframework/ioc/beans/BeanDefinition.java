package com.lightframework.ioc.beans;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BeanDefinition {

    private static final byte FLAG_SINGLETON = 0x01;
    private static final byte FLAG_PRIMARY   = 0x02;
    private static final byte FLAG_LAZY_INIT = 0x04;
    // Custom scope flag: when set, scopeName field holds the custom scope name
    private static final byte FLAG_CUSTOM_SCOPE = 0x08;

    // 共享空数组常量，避免重复分配
    private static final String[] EMPTY_DEPENDS_ON = new String[0];
    // 共享空字符串常量
    private static final String EMPTY_STRING = "";

    private String beanName;
    private Class<?> beanClass;
    private byte flags = FLAG_SINGLETON;
    // Custom scope name (only meaningful when FLAG_CUSTOM_SCOPE is set)
    private String scopeName = EMPTY_STRING;
    private String[] dependsOn = EMPTY_DEPENDS_ON;
    private String qualifier;
    private String factoryMethodName;
    private String factoryBeanName;
    private String initMethodName;
    private String destroyMethodName;
    private Map<String, Object> propertyValues;

    public BeanDefinition() {}

    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public String getScope() {
        if (isCustomScope()) {
            return scopeName;
        }
        return isSingleton() ? "singleton" : "prototype";
    }

    public void setScope(String scope) {
        if (scope == null || scope.isEmpty()) {
            scope = "singleton";
        }
        // Check if it's a built-in scope
        switch (scope) {
            case "singleton":
                flags |= FLAG_SINGLETON;
                flags &= ~FLAG_CUSTOM_SCOPE;
                scopeName = EMPTY_STRING;
                break;
            case "prototype":
                flags &= ~FLAG_SINGLETON;
                flags &= ~FLAG_CUSTOM_SCOPE;
                scopeName = EMPTY_STRING;
                break;
            default:
                // Custom scope (request, session, application, or user-defined)
                flags &= ~FLAG_SINGLETON; // Custom scopes are never singleton
                flags |= FLAG_CUSTOM_SCOPE;
                scopeName = scope.intern(); // String pooling for faster comparison
                break;
        }
    }

    /**
     * 检查是否为自定义作用域
     */
    public boolean isCustomScope() {
        return (flags & FLAG_CUSTOM_SCOPE) != 0;
    }

    /**
     * 获取原始作用域名称（不进行自定义/内置转换）
     */
    public String getScopeName() {
        return scopeName;
    }

    public boolean isSingleton() {
        return !isCustomScope() && (flags & FLAG_SINGLETON) != 0;
    }

    public boolean isPrototype() {
        return !isCustomScope() && (flags & FLAG_SINGLETON) == 0;
    }

    public boolean isPrimary() {
        return (flags & FLAG_PRIMARY) != 0;
    }

    public void setPrimary(boolean primary) {
        if (primary) flags |= FLAG_PRIMARY;
        else flags &= ~FLAG_PRIMARY;
    }

    public boolean isLazyInit() {
        return (flags & FLAG_LAZY_INIT) != 0;
    }

    public void setLazyInit(boolean lazyInit) {
        if (lazyInit) flags |= FLAG_LAZY_INIT;
        else flags &= ~FLAG_LAZY_INIT;
    }

    public String[] getDependsOn() {
        return dependsOn.length == 0 ? EMPTY_DEPENDS_ON : dependsOn.clone();
    }

    public void setDependsOn(String[] dependsOn) {
        this.dependsOn = dependsOn != null && dependsOn.length > 0 ? dependsOn.clone() : EMPTY_DEPENDS_ON;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public void setFactoryMethodName(String factoryMethodName) {
        this.factoryMethodName = factoryMethodName;
    }

    public String getFactoryBeanName() {
        return factoryBeanName;
    }

    public void setFactoryBeanName(String factoryBeanName) {
        this.factoryBeanName = factoryBeanName;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }

    public Map<String, Object> getPropertyValues() {
        if (propertyValues == null) {
            propertyValues = new HashMap<>();
        }
        return propertyValues;
    }

    public void setPropertyValue(String name, Object value) {
        getPropertyValues().put(name, value);
    }

    public Object getPropertyValue(String name) {
        return propertyValues != null ? propertyValues.get(name) : null;
    }
}
