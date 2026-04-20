package com.lightframework.ioc.exception;

public class NoSuchBeanDefinitionException extends BeansException {
    private final Class<?> requiredType;
    
    public NoSuchBeanDefinitionException(String name) {
        super("No bean named '" + name + "' available");
        this.requiredType = null;
    }
    
    public NoSuchBeanDefinitionException(Class<?> type) {
        super("No qualifying bean of type '" + type.getName() + "' available");
        this.requiredType = type;
    }
    
    public NoSuchBeanDefinitionException(Class<?> type, String message) {
        super(message);
        this.requiredType = type;
    }
    
    public Class<?> getRequiredType() {
        return this.requiredType;
    }
}