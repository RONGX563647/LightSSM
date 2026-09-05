package com.lightframework.aop;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProxyFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProxyFactoryTest {

    // CGLIB 运行时依赖底层 ClassLoader.defineClass，在高版本 JDK 上因模块未 opens java.lang 而不可用；
    // 此开关让涉及 CGLIB 的用例在该环境下优雅跳过（环境限制，非代码问题）。
    static final boolean CGLIB_OK = checkCglib();
    static boolean checkCglib() {
        try {
            net.sf.cglib.proxy.Enhancer e = new net.sf.cglib.proxy.Enhancer();
            e.setSuperclass(Object.class);
            e.create();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    interface Service { int add(int a, int b); }

    static class ServiceImpl implements Service {
        @Override public int add(int a, int b) { return a + b; }
    }

    static class Plain { public int add(int a, int b) { return a + b; } }

    static class LogInterceptor implements MethodInterceptor {
        final List<String> log;
        LogInterceptor(List<String> l) { this.log = l; }
        @Override
        public Object invoke(MethodInvocation inv) throws Throwable {
            log.add("before");
            try { return inv.proceed(); } finally { log.add("after"); }
        }
    }

    @Test
    void jdkProxyForInterfaceBean() {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （ProxyFactory.createAopProxy 的 JDK/CGLIB 选择逻辑，对应 ProxyFactory.java 的工厂方法练习）
        Service proxy = (Service) new ProxyFactory(new ServiceImpl()).getProxy();
        assertTrue(Proxy.isProxyClass(proxy.getClass()));
        assertEquals(5, proxy.add(2, 3));
    }

    @Test
    void cglibProxyForPlainClass() {
        Assumptions.assumeTrue(CGLIB_OK, "CGLIB 运行时在当前 JDK 不可用（模块未 opens java.lang），跳过");
        Plain proxy = (Plain) new ProxyFactory(new Plain()).getProxy();
        assertFalse(Proxy.isProxyClass(proxy.getClass()));
        assertTrue(proxy instanceof Plain);
        assertEquals(5, proxy.add(2, 3));
    }

    @Test
    void preferCglibForInterfaceBean() {
        Assumptions.assumeTrue(CGLIB_OK, "CGLIB 运行时在当前 JDK 不可用（模块未 opens java.lang），跳过");
        ProxyFactory pf = new ProxyFactory(new ServiceImpl());
        pf.setPreferCglib(true);
        Service proxy = (Service) pf.getProxy();
        assertFalse(Proxy.isProxyClass(proxy.getClass()));
        assertTrue(proxy instanceof ServiceImpl);
        assertEquals(5, proxy.add(2, 3));
    }

    @Test
    void interceptorRunsOnProxy() throws Throwable {
        List<String> log = new ArrayList<>();
        ProxyFactory pf = new ProxyFactory(new ServiceImpl());
        Method m = ServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new LogInterceptor(log));
        Service proxy = (Service) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of("before", "after"), log);
    }
}
