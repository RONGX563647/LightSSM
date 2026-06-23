package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = "PUT")
public @interface PutMapping {
    String value() default "";
    String[] path() default {};
}
