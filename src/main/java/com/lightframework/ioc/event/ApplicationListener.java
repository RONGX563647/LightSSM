package com.lightframework.ioc.event;

/**
 * Interface to be implemented by application event listeners.
 * Similar to Spring Framework's ApplicationListener.
 *
 * @param <T> the type of event this listener is interested in
 */
public interface ApplicationListener<T extends ApplicationEvent> {
    void onApplicationEvent(T event);
}
