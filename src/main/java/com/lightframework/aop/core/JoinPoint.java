package com.lightframework.aop.core;

import java.lang.reflect.Method;
import java.util.Arrays;

public class JoinPoint {
    private Object target;
    private Method method;
    private Object[] args;
    private Object proxy;
    
    public JoinPoint(Object target, Method method, Object[] args, Object proxy) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.proxy = proxy;
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
    
    public String getMethodName() {
        return this.method.getName();
    }
    
    public Class<?> getTargetClass() {
        return this.target.getClass();
    }
    
    public String getSignature() {
        return this.target.getClass().getName() + "." + this.method.getName();
    }
    
    public String getArgsString() {
        return Arrays.toString(this.args);
    }
    
    public void setArgs(Object[] args) {
        this.args = args;
    }
}