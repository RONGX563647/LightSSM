package com.lightframework.aop.core;

public class ProxyFactory {
    private AdvisedSupport advised;
    
    public ProxyFactory(Object target) {
        this.advised = new AdvisedSupport(target);
    }
    
    public Object getProxy() {
        return createAopProxy().getProxy();
    }
    
    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }
    
    protected AopProxy createAopProxy() {
        Class<?> targetClass = advised.getTargetClass();
        
        if (targetClass.getInterfaces().length > 0) {
            return new JdkDynamicAopProxy(advised);
        } else {
            return new CglibAopProxy(advised);
        }
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