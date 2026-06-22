package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 高性能 JDK 动态代理实现
 * 核心优化：
 * 1. 单次查找获取拦截器链索引 + MethodHandle 索引
 * 2. 缓存 target 引用
 * 3. 无拦截器快速路径
 * 4. 对象池 MethodInvocation
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
        this.advised.compile();
    }
    
    @Override
    public Object getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] proxiedInterfaces = advised.getTargetClass().getInterfaces();
        if (proxiedInterfaces.length == 0) {
            throw new IllegalArgumentException("Target class " + advised.getTargetClass()
                + " has no interfaces; JDK proxy cannot be used");
        }
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 快速路径：跳过 Object 方法
        if (method.getDeclaringClass() == Object.class) {
            if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(method.getName())) {
                return "JDK Proxy: " + advised.getTargetClass().getName();
            }
        }
        
        Object target = advised.getTarget();
        
        // 单次查找，返回索引
        int chainIdx = advised.getChainIndex(method);
        
        if (chainIdx < 0) {
            // 快速路径：无拦截器，直接调用 MethodHandle
            MethodHandle mh = advised.getTargetMethodHandle(method);
            if (mh != null) {
                return mh.invokeWithArguments(args);
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        
        // 使用索引直接访问（零重复查找）
        MethodInterceptor[] chain = advised.getInterceptorChains()[chainIdx];
        MethodHandle targetMethodHandle = advised.getTargetMethodHandles()[chainIdx];
        
        MethodInvocation invocation = MethodInvocation.obtain(
            target, method, args, proxy, chain, targetMethodHandle);
        
        try {
            return invocation.proceed();
        } finally {
            invocation.release();
        }
    }
}
