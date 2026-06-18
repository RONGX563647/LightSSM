package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Resource}
 */
@Deprecated
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resource {
    /**
     * Bean 名称，默认为字段名/方法名
     */
    String name() default "";

    /**
     * Bean 类型，默认为字段类型
     */
    Class<?> type() default Object.class;
}
