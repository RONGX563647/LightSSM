package com.lightframework.di.annotation;

import java.lang.annotation.*;

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
