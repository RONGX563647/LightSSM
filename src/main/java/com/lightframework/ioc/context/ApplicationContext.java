package com.lightframework.ioc.context;

import com.lightframework.ioc.core.ListableBeanFactory;

public interface ApplicationContext extends ListableBeanFactory {
    String getId();
    
    String getApplicationName();
    
    String getDisplayName();
    
    long getStartupDate();
    
    ApplicationContext getParent();
    
    void refresh() throws Exception;
    
    void close();
    
    boolean isActive();
}