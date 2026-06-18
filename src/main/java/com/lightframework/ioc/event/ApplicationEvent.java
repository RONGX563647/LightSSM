package com.lightframework.ioc.event;

/**
 * Base class for application events, similar to Spring Framework's ApplicationEvent.
 * All custom events should extend this class.
 */
public abstract class ApplicationEvent {
    private final Object source;
    private final long timestamp;
    
    public ApplicationEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Object getSource() {
        return source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
