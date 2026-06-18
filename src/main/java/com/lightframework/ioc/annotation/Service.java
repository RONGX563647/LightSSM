package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * Service 注解 - 标记业务层组件
 * 是 @Component 的派生注解，会被组件扫描器识别
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Service}
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Service {
    String value() default "";
}
