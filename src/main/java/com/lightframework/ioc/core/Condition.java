package com.lightframework.ioc.core;

/**
 * 条件接口
 * 用于 @Conditional 注解的条件判断
 */
public interface Condition {
    boolean matches(ConditionContext context);
}
