package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = "GET")
public @interface GetMapping {
    String value() default "";
    String[] path() default {};
}
