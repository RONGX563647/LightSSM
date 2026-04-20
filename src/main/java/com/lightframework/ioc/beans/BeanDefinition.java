package com.lightframework.ioc.beans;

public class BeanDefinition {
    private String beanName;
    private Class<?> beanClass;
    private String scope = "singleton";
    private boolean isPrimary = false;
    private boolean isLazyInit = false;
    private String[] dependsOn = new String[0];
    
    public BeanDefinition() {}
    
    public BeanDefinition(String beanName, Class<?> beanClass) {
        this.beanName = beanName;
        this.beanClass = beanClass;
    }
    
    public String getBeanName() {
        return beanName;
    }
    
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }
    
    public Class<?> getBeanClass() {
        return beanClass;
    }
    
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public boolean isSingleton() {
        return "singleton".equals(scope);
    }
    
    public boolean isPrototype() {
        return "prototype".equals(scope);
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    
    public boolean isLazyInit() {
        return isLazyInit;
    }
    
    public void setLazyInit(boolean isLazyInit) {
        this.isLazyInit = isLazyInit;
    }
    
    public String[] getDependsOn() {
        return dependsOn;
    }
    
    public void setDependsOn(String[] dependsOn) {
        this.dependsOn = dependsOn;
    }
}