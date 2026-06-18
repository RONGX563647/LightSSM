package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * 配置类注解
 * 兼容 Spring 的 @Configuration
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Configuration}
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    String value() default "";
}
