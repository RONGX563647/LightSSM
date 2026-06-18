package com.lightframework.di.annotation;

import java.lang.annotation.*;

/**
 * 配置类注解
 * 兼容 Spring 的 @Configuration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    String value() default "";
}
