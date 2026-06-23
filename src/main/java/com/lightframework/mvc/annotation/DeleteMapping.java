package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = "DELETE")
public @interface DeleteMapping {
    String value() default "";
    String[] path() default {};
}
