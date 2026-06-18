package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Scope}
 */
@Deprecated
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    String value() default SINGLETON;
    
    String SINGLETON = "singleton";
    String PROTOTYPE = "prototype";
    String REQUEST = "request";
    String SESSION = "session";
    String APPLICATION = "application";
}