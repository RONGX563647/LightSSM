package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CookieValue {
    String value() default "";
    String name() default "";
    boolean required() default true;
    String defaultValue() default "";
}
