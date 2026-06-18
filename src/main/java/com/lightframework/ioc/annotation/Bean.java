package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * Bean 方法注解
 * 兼容 Spring 的 @Bean
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Bean}
 */
@Deprecated
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {
    String[] value() default {};
    String[] name() default {};
    String initMethod() default "";
    String destroyMethod() default "(inferred)";
}
