package com.lightframework.ioc.beans;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BeanDefinition {

    private static final byte FLAG_SINGLETON = 0x01;
    private static final byte FLAG_PRIMARY   = 0x02;
    private static final byte FLAG_LAZY_INIT = 0x04;

    // 共享空数组常量，避免重复分配
    private static final String[] EMPTY_DEPENDS_ON = new String[0];

    private String beanName;
    private Class<?> beanClass;
    private byte flags = FLAG_SINGLETON;
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
        return isSingleton() ? "singleton" : "prototype";
    }

    public void setScope(String scope) {
        if ("singleton".equals(scope)) {
            flags |= FLAG_SINGLETON;
        } else {
            flags &= ~FLAG_SINGLETON;
        }
    }

    public boolean isSingleton() {
        return (flags & FLAG_SINGLETON) != 0;
    }

    public boolean isPrototype() {
        return (flags & FLAG_SINGLETON) == 0;
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
