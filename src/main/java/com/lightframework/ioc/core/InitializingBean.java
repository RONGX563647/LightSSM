package com.lightframework.ioc.core;

/**
 * Interface to be implemented by beans that need to react once all their
 * properties have been set by a BeanFactory.
 *
 * Implementing this interface is an alternative to the @PostConstruct annotation.
 * The @PostConstruct method is called before afterPropertiesSet().
 */
public interface InitializingBean {

    /**
     * Invoked by the containing BeanFactory after it has set all bean properties.
     *
     * @throws Exception in case of initialization failure
     */
    void afterPropertiesSet() throws Exception;
}
