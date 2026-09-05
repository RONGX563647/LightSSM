package com.lightframework.di.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

// TODO [L1][练习] 手写 @Autowired 的 required 语义——当 required()=true 且容器中无匹配依赖时注入引擎应抛 DependencyResolutionException；required()=false 缺失时注入引擎应跳过并保持字段为 null；写对标志：可选依赖缺失时 bean 正常创建且字段为 null，必需依赖缺失时抛异常。
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    boolean required() default true;
}
