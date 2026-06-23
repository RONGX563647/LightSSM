package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {
    int value() default 200;
    String reason() default "";
}
