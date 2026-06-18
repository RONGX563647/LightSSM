package com.lightframework.di.core;

public class DependencyResolutionException extends RuntimeException {
    private final String beanName;

    public DependencyResolutionException(String beanName, String message) {
        super(message);
        this.beanName = beanName;
    }

    public DependencyResolutionException(String beanName, String message, Throwable cause) {
        super(message, cause);
        this.beanName = beanName;
    }

    public String getBeanName() {
        return beanName;
    }
}
