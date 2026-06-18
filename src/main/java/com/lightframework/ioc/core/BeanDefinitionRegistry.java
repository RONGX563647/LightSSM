package com.lightframework.ioc.core;

import com.lightframework.ioc.beans.BeanDefinition;

/**
 * BeanDefinition 注册表接口
 * 兼容 Spring 的 BeanDefinitionRegistry
 */
public interface BeanDefinitionRegistry {
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
    void removeBeanDefinition(String beanName) throws Exception;
    BeanDefinition getBeanDefinition(String beanName);
    boolean containsBeanDefinition(String beanName);
    String[] getBeanDefinitionNames();
    int getBeanDefinitionCount();
    void registerAlias(String beanName, String alias);
}
