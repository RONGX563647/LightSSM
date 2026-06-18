package com.lightframework.ioc.event;

/**
 * Interface that encapsulates event publication functionality.
 * Similar to Spring Framework's ApplicationEventPublisher.
 */
public interface ApplicationEventPublisher {
    void publishEvent(ApplicationEvent event);
}
