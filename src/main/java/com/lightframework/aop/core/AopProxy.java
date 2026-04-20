package com.lightframework.aop.core;

import java.lang.reflect.Method;

public interface AopProxy {
    Object getProxy();
    
    Object getProxy(ClassLoader classLoader);
}