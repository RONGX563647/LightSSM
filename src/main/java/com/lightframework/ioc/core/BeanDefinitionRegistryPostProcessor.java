package com.lightframework.ioc.core;

/**
 * BeanDefinition 注册后置处理器 SPI
 * 在 BeanFactoryPostProcessor 之前执行，用于动态注册 BeanDefinition
 * 兼容 Spring 的 BeanDefinitionRegistryPostProcessor
 */
public interface BeanDefinitionRegistryPostProcessor extends BeanFactoryPostProcessor {
    /**
     * 在 BeanFactoryPostProcessor 之前执行
     * 用于动态注册 BeanDefinition
     */
    void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws Exception;
}
