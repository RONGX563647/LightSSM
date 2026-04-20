package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProceedingJoinPoint;
import java.lang.reflect.Method;

public class AroundMethodInterceptor implements MethodInterceptor {
    
    private Method aspectMethod;
    private Object aspectInstance;
    
    public AroundMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        this.aspectMethod = aspectMethod;
        this.aspectInstance = aspectInstance;
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ProceedingJoinPoint joinPoint = new ProceedingJoinPoint(
            invocation.getTarget(),
            invocation.getMethod(),
            invocation.getArgs(),
            invocation.getProxy()
        );
        
        aspectMethod.setAccessible(true);
        return aspectMethod.invoke(aspectInstance, joinPoint);
    }
}