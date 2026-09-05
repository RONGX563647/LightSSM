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

public class CglibAopProxyTest {

    // 同 ProxyFactoryTest：CGLIB 在高版本 JDK 上因模块未 opens java.lang 而运行时不可用，环境限制则跳过。
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
    void cglibToString() {
        Assumptions.assumeTrue(CGLIB_OK, "CGLIB 运行时在当前 JDK 不可用（模块未 opens java.lang），跳过");
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿
        // （CglibAopProxy.handleObjectMethod 对 Object 方法的透传处理）
        Plain proxy = (Plain) new ProxyFactory(new Plain()).getProxy();
        assertTrue(proxy.toString().startsWith("CGLIB Proxy:"));
        assertFalse(Proxy.isProxyClass(proxy.getClass()));
    }

    @Test
    void cglibRunsInterceptor() throws Throwable {
        Assumptions.assumeTrue(CGLIB_OK, "CGLIB 运行时在当前 JDK 不可用（模块未 opens java.lang），跳过");
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （CglibAopProxy 代理创建 + 拦截器织入；对应 CglibAopProxy.java 代理类缓存练习）
        List<String> log = new ArrayList<>();
        ProxyFactory pf = new ProxyFactory(new Plain());
        Method m = Plain.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new LogInterceptor(log));
        Plain proxy = (Plain) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of("before", "after"), log);
    }

    @Test
    void repeatedGetProxySameType() {
        Assumptions.assumeTrue(CGLIB_OK, "CGLIB 运行时在当前 JDK 不可用（模块未 opens java.lang），跳过");
        ProxyFactory pf = new ProxyFactory(new Plain());
        Plain p1 = (Plain) pf.getProxy();
        Plain p2 = (Plain) pf.getProxy();
        assertEquals(p1.getClass(), p2.getClass());
        assertEquals(5, p1.add(1, 1));
        assertEquals(5, p2.add(1, 1));
    }
}
