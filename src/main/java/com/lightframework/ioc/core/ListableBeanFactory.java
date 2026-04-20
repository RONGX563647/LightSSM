package com.lightframework.ioc.core;

import com.lightframework.ioc.beans.BeanDefinition;
import java.util.List;

public interface ListableBeanFactory extends BeanFactory {
    boolean containsBeanDefinition(String beanName);
    
    int getBeanDefinitionCount();
    
    String[] getBeanDefinitionNames();
    
    String[] getBeanNamesForType(Class<?> type);
    
    <T> List<T> getBeansOfType(Class<T> type) throws Exception;
    
    <T> T getPrimaryBean(Class<T> type) throws Exception;
}