package com.lightframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Pointcut {
    // TODO [L1][练习] 理解 @Pointcut 命名切点——advice 的 value 引用该命名方法(如 "servicePC()")时需；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // AspectJAutoProxyCreator.resolvePointcut 从 pointcutMap 解析；写测试验证命名引用与内联 execution 两种写法等价。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AnnotationUsageTest）。
    String value();
}