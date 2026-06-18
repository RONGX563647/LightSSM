package com.lightframework.spi.annotation;

import com.lightframework.di.annotation.Conditional;
import com.lightframework.spi.condition.OnMissingBeanCondition;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnMissingBeanCondition.class)
public @interface ConditionalOnMissingBean {
    Class<?>[] value() default {};
}
