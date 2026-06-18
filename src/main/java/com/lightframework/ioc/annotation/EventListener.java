package com.lightframework.ioc.annotation;

import com.lightframework.ioc.event.ApplicationEvent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that marks a method as an event listener.
 * Similar to Spring Framework's @EventListener annotation.
 * The annotated method will be invoked when a matching event is published.
 * @deprecated 移到了 {@link com.lightframework.di.annotation.EventListener}
 */
@Deprecated
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    /**
     * The event type to listen for.
     * Defaults to ApplicationEvent.class (listens for all events).
     */
    Class<? extends ApplicationEvent> value() default ApplicationEvent.class;
}
