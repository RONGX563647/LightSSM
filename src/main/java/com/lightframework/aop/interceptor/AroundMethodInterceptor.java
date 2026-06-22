package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProceedingJoinPoint;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * 环绕通知拦截器
 * 包裹目标方法执行，可控制目标方法是否执行、何时执行、参数修改
 * 
 * 修复：创建 ProceedingJoinPoint 包装当前 MethodInvocation
 * 当切面调用 joinPoint.proceed() 时，正确传递到下一个拦截器
 */
public class AroundMethodInterceptor implements MethodInterceptor {

    private final MethodHandle aspectMethodHandle;
    private final MethodHandle adaptedHandle;

    public AroundMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        try {
            MethodHandle mh = MethodHandles.lookup().unreflect(aspectMethod);
            this.aspectMethodHandle = mh.bindTo(aspectInstance);
            this.adaptedHandle = this.aspectMethodHandle.asType(
                MethodType.methodType(Object.class, ProceedingJoinPoint.class));
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to create MethodHandle for aspect method: " + aspectMethod, e);
        }
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 创建 ProceedingJoinPoint 包装当前 MethodInvocation
        ProceedingJoinPoint joinPoint = new ProceedingJoinPoint(invocation);

        return adaptedHandle.invokeExact(joinPoint);
    }
}
