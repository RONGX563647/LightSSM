package com.lightframework.aop;

import com.lightframework.aop.core.ProxyFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

public class JdkDynamicAopProxyTest {

    interface Service { int add(int a, int b); String name(); }

    static class ServiceImpl implements Service {
        @Override public int add(int a, int b) { return a + b; }
        @Override public String name() { return "svc"; }
    }

    @Test
    void proxyToStringAndEqualsHashCode() {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （JdkDynamicAopProxy.invoke 的 Object 方法 equals/hashCode/toString 处理）
        Service proxy = (Service) new ProxyFactory(new ServiceImpl()).getProxy();
        assertTrue(proxy.toString().startsWith("JDK Proxy:"));
        Service other = (Service) new ProxyFactory(new ServiceImpl()).getProxy();
        assertEquals(proxy, proxy);
        assertNotEquals(proxy, other);
    }

    @Test
    void getClassIsProxyClass() {
        Service proxy = (Service) new ProxyFactory(new ServiceImpl()).getProxy();
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        assertNotSame(ServiceImpl.class, proxy.getClass());
    }

    @Test
    void businessMethodInvoked() {
        Service proxy = (Service) new ProxyFactory(new ServiceImpl()).getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals("svc", proxy.name());
    }
}
