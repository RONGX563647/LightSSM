package com.lightframework.ioc.core;

public interface BeanFactoryAware extends Aware {
    void setBeanFactory(DefaultListableBeanFactory beanFactory);
}
