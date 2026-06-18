package com.lightframework.di.annotation;

import java.lang.annotation.*;

/**
 * Repository 注解 - 标记数据访问层组件
 * 是 @Component 的派生注解，会被组件扫描器识别
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Repository {
    String value() default "";
}
