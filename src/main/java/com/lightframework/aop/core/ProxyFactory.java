package com.lightframework.aop.core;

import java.util.List;

public class ProxyFactory {

    private AdvisedSupport advised;
    private boolean preferCglib = false;

    public ProxyFactory(Object target) {
        this.advised = new AdvisedSupport(target);
    }

    public void setPreferCglib(boolean preferCglib) {
        this.preferCglib = preferCglib;
    }

    public Object getProxy() {
        return createAopProxy().getProxy();
    }

    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }

    protected AopProxy createAopProxy() {
        Class<?> targetClass = advised.getTargetClass();

        if (preferCglib || targetClass.getInterfaces().length == 0) {
            if (java.lang.reflect.Modifier.isFinal(targetClass.getModifiers())) {
                throw new IllegalArgumentException("Cannot create CGLIB proxy for final class: "
                    + targetClass.getName() + ". Use JDK proxy by not setting preferCglib.");
            }
            return new CglibAopProxy(advised);
        }

        return new JdkDynamicAopProxy(advised);
    }

    public void addInterceptor(java.lang.reflect.Method method, MethodInterceptor interceptor) {
        this.advised.addInterceptor(method, interceptor);
    }

    public void addInterceptors(java.lang.reflect.Method method, List<MethodInterceptor> interceptors) {
        this.advised.addInterceptors(method, interceptors);
    }

    public AdvisedSupport getAdvised() {
        return this.advised;
    }
}