package com.lightframework.ioc.exception;

public class BeanCreationException extends BeansException {
    private final String beanName;
    
    public BeanCreationException(String beanName, String msg) {
        super("Error creating bean with name '" + beanName + "': " + msg);
        this.beanName = beanName;
    }
    
    public BeanCreationException(String beanName, String msg, Throwable cause) {
        super("Error creating bean with name '" + beanName + "': " + msg, cause);
        this.beanName = beanName;
    }
    
    public String getBeanName() {
        return this.beanName;
    }
}