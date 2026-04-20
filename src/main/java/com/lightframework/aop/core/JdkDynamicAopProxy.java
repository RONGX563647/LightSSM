package com.lightframework.aop.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }
    
    @Override
    public Object getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] proxiedInterfaces = advised.getTargetClass().getInterfaces();
        if (proxiedInterfaces.length == 0) {
            proxiedInterfaces = new Class<?>[]{advised.getTargetClass()};
        }
        
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object target = advised.getTarget();
        Class<?> targetClass = advised.getTargetClass();
        
        List<MethodInterceptor> interceptors = advised.getInterceptors(method);
        
        if (interceptors == null || interceptors.isEmpty()) {
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        
        MethodInvocation invocation = new MethodInvocation(target, method, args, proxy, interceptors);
        return invocation.proceed();
    }
}