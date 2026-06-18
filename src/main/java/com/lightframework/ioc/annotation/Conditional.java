package com.lightframework.ioc.annotation;

import com.lightframework.ioc.core.Condition;

import java.lang.annotation.*;

/**
 * 条件注册注解
 * 兼容 Spring 的 @Conditional
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Conditional}
 */
@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Conditional {
    Class<? extends Condition>[] value();
}
