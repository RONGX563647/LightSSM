package com.lightframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Before {
    // TODO [L1][练习] 为 @Before 增加 pointcut 别名属性(如 value 与 pointcut 二选一，类似 Spring 的 @AliasFor)，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 并写测试验证两种写法都生效。验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AnnotationUsageTest）。
    String value();
}