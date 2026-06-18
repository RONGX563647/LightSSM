package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * Indicates which profile(s) a component or configuration class is compatible with.
 * Only registered when at least one specified profile is active in the Environment.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Profile {
    String[] value();
}
