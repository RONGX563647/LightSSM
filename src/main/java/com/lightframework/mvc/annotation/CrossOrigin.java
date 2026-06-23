package com.lightframework.mvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {
    String[] origins() default {};
    String[] allowedHeaders() default {};
    String[] exposedHeaders() default {};
    String[] methods() default {};
    boolean allowCredentials() default true;
    long maxAge() default -1;
}
