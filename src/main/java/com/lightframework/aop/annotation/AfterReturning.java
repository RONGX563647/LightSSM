package com.lightframework.aop.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AfterReturning {
    // TODO [L1][练习] 理解 @AfterReturning.returning()——需把目标返回值绑定到通知参数；写下目标返回值类型与通知参数；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 类型不匹配时行为的测试(当前拦截器未消费 returning)。验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿
    // （AnnotationUsageTest）。
    String value();
    String returning() default "";
}