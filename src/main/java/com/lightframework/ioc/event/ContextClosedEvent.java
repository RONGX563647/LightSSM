package com.lightframework.ioc.event;

/**
 * Event published when the ApplicationContext is closed.
 * Similar to Spring Framework's ContextClosedEvent.
 */
public class ContextClosedEvent extends ApplicationEvent {
    
    public ContextClosedEvent(Object source) {
        super(source);
    }
}
