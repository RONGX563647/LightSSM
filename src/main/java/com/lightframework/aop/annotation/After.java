package com.lightframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface After {
    // TODO [L1][练习] 理解 @After 与 @AfterReturning/@AfterThrowing 的区别——@After 在 finally 中执行(无论成功/异常)、；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // @AfterReturning 仅成功时；写测试验证目标抛异常时 @After 仍执行而 @AfterReturning 不执行。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AnnotationUsageTest）。
    String value();
}