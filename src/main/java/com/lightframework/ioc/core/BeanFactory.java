package com.lightframework.ioc.core;

public interface BeanFactory {
    Object getBean(String name) throws Exception;
    
    <T> T getBean(String name, Class<T> requiredType) throws Exception;
    
    <T> T getBean(Class<T> requiredType) throws Exception;
    
    boolean containsBean(String name);
    
    boolean isSingleton(String name);
    
    boolean isPrototype(String name);
    
    Class<?> getType(String name);
    
    String[] getAliases(String name);
}