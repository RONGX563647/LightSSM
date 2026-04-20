package com.lightframework.aop.core;

public class ProceedingJoinPoint extends JoinPoint {
    
    public ProceedingJoinPoint(Object target, java.lang.reflect.Method method, 
        Object[] args, Object proxy) {
        super(target, method, args, proxy);
    }
    
    public Object proceed() throws Throwable {
        return proceed(getArgs());
    }
    
    public Object proceed(Object[] args) throws Throwable {
        getMethod().setAccessible(true);
        return getMethod().invoke(getTarget(), args);
    }
}