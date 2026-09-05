package com.lightframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Around {
    // TODO [L1][练习] 理解 @Around 约束——@Around 通知方法必须返回 Object 且首参为 ProceedingJoinPoint；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 写测试验证：若方法签名缺 ProceedingJoinPoint，AroundMethodInterceptor 构造/调用会抛清晰异常。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AnnotationUsageTest）。
    String value();
}