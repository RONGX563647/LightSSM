package com.lightframework.ioc.core;

/**
 * Interface to be implemented by beans that want to release resources
 * on destruction. A BeanFactory is expected to invoke the destroy method
 * when it disposes a cached singleton.
 *
 * Implementing this interface is an alternative to the @PreDestroy annotation.
 * The @PreDestroy method is called before destroy().
 */
public interface DisposableBean {

    /**
     * Invoked by the containing BeanFactory on destruction of a bean.
     *
     * @throws Exception in case of cleanup failure
     */
    void destroy() throws Exception;
}
