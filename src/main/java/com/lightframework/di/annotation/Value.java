package com.lightframework.di.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// TODO [L1][练习] 手写 @Value 占位符提取工具——从注解 value 中正则提取 ${key:default} 的 key 与 default（如 "${app.port:8080}" → key=app.port, default=8080）；写对标志：无冒号时 default 为空、无 ${} 包裹时原样返回。该提取逻辑供 PlaceholderResolver 在解析前预处理。
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value() default "";
}
