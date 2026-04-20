package com.lightframework.aop.core;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdvisedSupport {
    private Object target;
    private Class<?> targetClass;
    private Map<Method, List<MethodInterceptor>> methodInterceptors = new ConcurrentHashMap<>();
    
    public AdvisedSupport(Object target) {
        this.target = target;
        this.targetClass = target.getClass();
    }
    
    public Object getTarget() {
        return this.target;
    }
    
    public void setTarget(Object target) {
        this.target = target;
        this.targetClass = target.getClass();
    }
    
    public Class<?> getTargetClass() {
        return this.targetClass;
    }
    
    public void addInterceptor(Method method, MethodInterceptor interceptor) {
        List<MethodInterceptor> interceptors = this.methodInterceptors.computeIfAbsent(
            method, k -> new java.util.ArrayList<>());
        interceptors.add(interceptor);
    }
    
    public void addInterceptors(Method method, List<MethodInterceptor> interceptors) {
        List<MethodInterceptor> existingInterceptors = this.methodInterceptors.computeIfAbsent(
            method, k -> new java.util.ArrayList<>());
        existingInterceptors.addAll(interceptors);
    }
    
    public List<MethodInterceptor> getInterceptors(Method method) {
        return this.methodInterceptors.get(method);
    }
    
    public Map<Method, List<MethodInterceptor>> getMethodInterceptors() {
        return this.methodInterceptors;
    }
    
    public boolean hasInterceptors() {
        return !this.methodInterceptors.isEmpty();
    }
}