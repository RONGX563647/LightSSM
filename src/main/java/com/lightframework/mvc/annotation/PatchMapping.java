package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = "PATCH")
public @interface PatchMapping {
    String value() default "";
    String[] path() default {};
}
