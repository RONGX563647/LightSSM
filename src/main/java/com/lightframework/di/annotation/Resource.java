package com.lightframework.di.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// TODO [L1][练习] 手写 @Resource 的 name/type 解析规则说明与判定——name 属性非空时优先按名称查找，否则按字段/参数类型（type 属性或声明类型）查找；写对标志：该规则与 InjectionEngine.resolveResourceByNameOrType 的「先 name 后 type」行为一致。
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resource {
    String name() default "";
    Class<?> type() default Object.class;
}
