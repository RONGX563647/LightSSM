package com.lightframework.aop.core;

import java.lang.reflect.Method;
import java.util.List;

public class MethodInvocation {
    private Object target;
    private Method method;
    private Object[] args;
    private Object proxy;
    private List<MethodInterceptor> interceptors;
    private int currentInterceptorIndex = -1;
    
    public MethodInvocation(Object target, Method method, Object[] args, Object proxy, 
        List<MethodInterceptor> interceptors) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.proxy = proxy;
        this.interceptors = interceptors;
    }
    
    public Object proceed() throws Throwable {
        currentInterceptorIndex++;
        
        if (currentInterceptorIndex < interceptors.size()) {
            MethodInterceptor interceptor = interceptors.get(currentInterceptorIndex);
            return interceptor.invoke(this);
        }
        
        method.setAccessible(true);
        return method.invoke(target, args);
    }
    
    public Object getTarget() {
        return this.target;
    }
    
    public Method getMethod() {
        return this.method;
    }
    
    public Object[] getArgs() {
        return this.args;
    }
    
    public Object getProxy() {
        return this.proxy;
    }
    
    public JoinPoint getJoinPoint() {
        return new ProceedingJoinPoint(target, method, args, proxy);
    }
}