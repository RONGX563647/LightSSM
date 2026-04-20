package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    String value() default "singleton";
    
    String SINGLETON = "singleton";
    String PROTOTYPE = "prototype";
}