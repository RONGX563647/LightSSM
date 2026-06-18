package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Autowired}
 */
@Deprecated
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    boolean required() default true;
}