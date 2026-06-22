package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

public class AfterReturningMethodInterceptor implements MethodInterceptor {

    private final MethodHandle adaptedHandle;

    public AfterReturningMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            mh = mh.bindTo(aspectInstance);
            this.adaptedHandle = mh.asType(MethodType.methodType(Object.class, JoinPoint.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result = invocation.proceed();
        JoinPoint joinPoint = invocation.getJoinPoint();
        adaptedHandle.invokeExact(joinPoint);
        return result;
    }
}