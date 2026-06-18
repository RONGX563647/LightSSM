package com.lightframework.ioc.core;

/**
 * ImportBeanDefinitionRegistrar 接口：用于以编程方式注册 BeanDefinition。
 * 实现该接口的类可以通过 registerBeanDefinitions() 方法直接操作 BeanFactory。
 */
public interface ImportBeanDefinitionRegistrar {
    
    /**
     * 注册 BeanDefinitions。
     * @param registry BeanFactory 注册表
     */
    void registerBeanDefinitions(DefaultListableBeanFactory registry);
}
