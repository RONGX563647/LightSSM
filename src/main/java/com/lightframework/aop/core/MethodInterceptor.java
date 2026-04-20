package com.lightframework.aop.core;

public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}