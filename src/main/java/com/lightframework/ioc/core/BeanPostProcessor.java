package com.lightframework.ioc.core;

public interface BeanPostProcessor {
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
    
    default Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
    
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }
}