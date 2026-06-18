package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * 注解用于指定 BeanPostProcessor 的执行顺序
 * 值越小，优先级越高（越先执行）
 * 默认值为 0
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Order}
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Order {
    int value() default 0;
}
