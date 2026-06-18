package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * Controller 注解 - 标记控制器层组件
 * 是 @Component 的派生注解，会被组件扫描器识别
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Controller {
    String value() default "";
}
