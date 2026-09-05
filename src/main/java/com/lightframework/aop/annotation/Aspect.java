package com.lightframework.aop.annotation;

import com.lightframework.di.annotation.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Aspect {
    // TODO [L1][练习] 理解 @Aspect 被 @Component 元注解修饰——IOC 扫描时应把切面也注册为 Bean；写测试验证带 @Aspect；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 的类会被容器管理、且 AspectJAutoProxyCreator(也是 @Component)能拿到它。验证 TODO[L1] 练习目标：用户手写实现后
    // 运行本测试应全绿（AnnotationUsageTest）。
    String value() default "";
}