package com.lightframework.ioc.core;

/**
 * 条件上下文
 * 提供条件判断所需的环境信息
 */
public interface ConditionContext {
    BeanDefinitionRegistry getRegistry();
    Environment getEnvironment();
    ClassLoader getClassLoader();
}
