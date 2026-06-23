package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = "POST")
public @interface PostMapping {
    String value() default "";
    String[] path() default {};
}
