package com.lightframework.aop.test;

import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public int multiply(int a, int b) {
        return a * b;
    }
}

class LoggingInterceptor implements MethodInterceptor {
    private StringBuilder log = new StringBuilder();
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        log.append("Before: ").append(invocation.getMethod().getName()).append("\n");
        Object result = invocation.proceed();
        log.append("After: ").append(invocation.getMethod().getName()).append("\n");
        return result;
    }
    
    public String getLog() {
        return log.toString();
    }
}

public class AopTest {
    
    @Test
    void testProxyCreation() {
        Calculator target = new Calculator();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        
        Calculator proxy = (Calculator) proxyFactory.getProxy();
        assertNotNull(proxy);
        assertTrue(Calculator.class.isInstance(proxy));
    }
    
    @Test
    void testMethodInterceptor() throws Throwable {
        Calculator target = new Calculator();
        LoggingInterceptor interceptor = new LoggingInterceptor();
        
        ProxyFactory proxyFactory = new ProxyFactory(target);
        try {
            java.lang.reflect.Method addMethod = Calculator.class.getMethod("add", int.class, int.class);
            proxyFactory.addInterceptor(addMethod, interceptor);
        } catch (NoSuchMethodException e) {
            fail("Method not found: " + e.getMessage());
        }
        
        Calculator proxy = (Calculator) proxyFactory.getProxy();
        int result = proxy.add(2, 3);
        
        assertEquals(5, result);
    }
}