package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Primary}
 */
@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Primary {
}
