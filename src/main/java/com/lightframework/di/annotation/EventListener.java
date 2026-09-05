package com.lightframework.di.annotation;

import com.lightframework.ioc.event.ApplicationEvent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个方法为事件监听器（对应 Spring 的 {@code @EventListener}）。
 *
 * <p>被标注的方法会在匹配事件发布时被调用，事件类型由 {@link #value()} 指定，
 * 默认监听所有 {@link ApplicationEvent}。</p>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {
    /**
     * 监听的事件类型，默认为 ApplicationEvent.class（监听所有事件）。
     */
    Class<? extends ApplicationEvent> value() default ApplicationEvent.class;
}
