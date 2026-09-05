package com.lightframework.ioc.core;

/**
 * 条件上下文
 * 提供条件判断所需的环境信息
 */
public interface ConditionContext {
    // TODO [L2][练习] 手写 @Conditional 条件评估框架 —— 定义 Condition 接口 + 在注册 Bean 前调用 condition.matches(context) 决定是否跳过。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：实现 OnClassCondition（类路径存在某类才注册），当该类缺失时对应 Bean 不被注册。
    BeanDefinitionRegistry getRegistry();
    Environment getEnvironment();
    ClassLoader getClassLoader();
}
