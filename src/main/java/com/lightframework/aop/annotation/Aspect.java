package com.lightframework.aop.annotation;

import com.lightframework.di.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Aspect {
    String value() default "";
}