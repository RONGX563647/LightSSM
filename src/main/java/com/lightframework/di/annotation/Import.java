package com.lightframework.di.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Import 注解：用于导入一个或多个配置类、ImportSelector 或 ImportBeanDefinitionRegistrar 实现。
 * 类似 Spring Framework 的 @Import 机制，支持模块化配置。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {
    Class<?>[] value();
}
