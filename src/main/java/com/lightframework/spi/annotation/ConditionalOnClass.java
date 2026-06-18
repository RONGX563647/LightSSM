package com.lightframework.spi.annotation;

import com.lightframework.ioc.annotation.Conditional;
import com.lightframework.spi.condition.OnClassCondition;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnClassCondition.class)
public @interface ConditionalOnClass {
    String[] value();
}
