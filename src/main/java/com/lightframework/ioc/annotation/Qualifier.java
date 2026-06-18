package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Qualifier}
 */
@Deprecated
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Qualifier {
    String value() default "";
}
