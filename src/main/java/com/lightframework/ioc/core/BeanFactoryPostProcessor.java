package com.lightframework.ioc.core;

/**
 * Interface to be implemented by beans that need to modify the bean factory's
 * internal structure, typically by registering custom property editors or
 * modifying bean definitions.
 *
 * <p>BeanFactoryPostProcessor implementations are invoked after all bean
 * definitions have been loaded but before any beans are instantiated.</p>
 *
 * <p>This is compatible with Spring's org.springframework.beans.factory.config.BeanFactoryPostProcessor</p>
 */
public interface BeanFactoryPostProcessor {

    /**
     * Modify the application context's internal bean factory after its
     * standard initialization.
     *
     * @param beanFactory the bean factory used by the application context
     * @throws Exception in case of errors
     */
    void postProcessBeanFactory(DefaultListableBeanFactory beanFactory) throws Exception;
}
