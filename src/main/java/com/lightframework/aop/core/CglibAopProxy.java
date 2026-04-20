package com.lightframework.aop.core;

import java.lang.reflect.Method;
import java.util.List;

public class CglibAopProxy implements AopProxy {
    
    private final AdvisedSupport advised;
    
    public CglibAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }
    
    @Override
    public Object getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Object target = advised.getTarget();
        Class<?> targetClass = advised.getTargetClass();
        
        try {
            net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
            enhancer.setClassLoader(classLoader);
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new CglibMethodInterceptor(advised));
            
            return enhancer.create();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CGLIB proxy", e);
        }
    }
    
    private static class CglibMethodInterceptor implements net.sf.cglib.proxy.MethodInterceptor {
        private final AdvisedSupport advised;
        
        public CglibMethodInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }
        
        @Override
        public Object intercept(Object obj, Method method, Object[] args, 
            net.sf.cglib.proxy.MethodProxy proxy) throws Throwable {
            
            Object target = advised.getTarget();
            List<MethodInterceptor> interceptors = advised.getInterceptors(method);
            
            if (interceptors == null || interceptors.isEmpty()) {
                return proxy.invoke(target, args);
            }
            
            MethodInvocation invocation = new MethodInvocation(target, method, args, obj, interceptors);
            return invocation.proceed();
        }
    }
}