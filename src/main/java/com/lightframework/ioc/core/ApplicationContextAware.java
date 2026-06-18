package com.lightframework.ioc.core;

import com.lightframework.ioc.context.ApplicationContext;

public interface ApplicationContextAware extends Aware {
    void setApplicationContext(ApplicationContext applicationContext);
}
