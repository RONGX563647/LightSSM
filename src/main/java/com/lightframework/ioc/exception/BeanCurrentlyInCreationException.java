package com.lightframework.ioc.exception;

public class BeanCurrentlyInCreationException extends BeansException {
    public BeanCurrentlyInCreationException(String beanName) {
        super("Bean '" + beanName + "' is currently in creation: Is there an unresolvable circular reference?");
    }

    public BeanCurrentlyInCreationException(String beanName, String message) {
        super("Bean '" + beanName + "' is currently in creation.\n" + message);
    }
}