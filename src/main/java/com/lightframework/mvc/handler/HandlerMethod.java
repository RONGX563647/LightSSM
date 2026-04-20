package com.lightframework.mvc.handler;

import com.lightframework.ioc.context.ApplicationContext;

import java.lang.reflect.Method;

public class HandlerMethod {
    private final String beanName;
    private final Method method;
    private final Class<?> beanType;
    private final ApplicationContext applicationContext;
    
    private Object handler;
    
    public HandlerMethod(String beanName, Method method, ApplicationContext applicationContext) 
        throws Exception {
        this.beanName = beanName;
        this.method = method;
        this.applicationContext = applicationContext;
        this.beanType = applicationContext.getType(beanName);
        this.handler = applicationContext.getBean(beanName);
    }
    
    public Object getBean() {
        return this.handler;
    }
    
    public Method getMethod() {
        return this.method;
    }
    
    public Class<?> getBeanType() {
        return this.beanType;
    }
    
    public String getBeanName() {
        return this.beanName;
    }
    
    public Class<?> getReturnType() {
        return this.method.getReturnType();
    }
    
    @Override
    public String toString() {
        return this.beanType.getSimpleName() + "#" + this.method.getName();
    }
}