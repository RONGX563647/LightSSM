package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * 后置通知拦截器
 * 在目标方法执行后运行（无论是否抛出异常）
 */
public class AfterMethodInterceptor implements MethodInterceptor {

    private final MethodHandle aspectMethodHandle;
    private final MethodHandle adaptedHandle;
    private final boolean takesJoinPoint;

    public AfterMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        this.takesJoinPoint = aspectMethod.getParameterCount() > 0;

        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            this.aspectMethodHandle = mh.bindTo(aspectInstance);
            this.adaptedHandle = takesJoinPoint
                ? this.aspectMethodHandle.asType(MethodType.methodType(Object.class, JoinPoint.class))
                : this.aspectMethodHandle.asType(MethodType.methodType(Object.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object result;
        try {
            result = invocation.proceed();
        } finally {
            if (takesJoinPoint) {
                JoinPoint joinPoint = invocation.getJoinPoint();
                adaptedHandle.invokeExact(joinPoint);
            } else {
                adaptedHandle.invokeExact();
            }
        }
        return result;
    }
}
