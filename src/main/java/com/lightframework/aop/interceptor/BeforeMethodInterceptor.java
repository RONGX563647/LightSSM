package com.lightframework.aop.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.JoinPoint;
import java.lang.reflect.Method;

public class BeforeMethodInterceptor implements MethodInterceptor {
    
    private Method aspectMethod;
    private Object aspectInstance;
    
    public BeforeMethodInterceptor(Method aspectMethod, Object aspectInstance) {
        this.aspectMethod = aspectMethod;
        this.aspectInstance = aspectInstance;
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        JoinPoint joinPoint = invocation.getJoinPoint();
        
        aspectMethod.setAccessible(true);
        aspectMethod.invoke(aspectInstance, joinPoint);
        
        return invocation.proceed();
    }
}