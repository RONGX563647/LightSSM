package com.lightframework.aop.test;

import com.lightframework.aop.core.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AOP 集成测试 - 验证实际可用性
 */
public class AopIntegrationTest {

    static interface BusinessService {
        int add(int a, int b);
        int multiply(int a, int b);
        void fail();
    }

    static class OrderServiceImpl implements BusinessService {
        @Override public int add(int a, int b) { return a + b; }
        public int multiply(int a, int b) { return a * b; }
        public void fail() { throw new RuntimeException("Intentional failure"); }
    }

    static class PlainService {
        public int add(int a, int b) { return a + b; }
    }

    // ==================== 拦截器 ====================

    static class LoggingInterceptor implements MethodInterceptor {
        final String name;
        final List<String> log;
        LoggingInterceptor(String name, List<String> log) { this.name = name; this.log = log; }
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            log.add(name + "-Before");
            try {
                Object result = invocation.proceed();
                return result;
            } finally {
                log.add(name + "-After");
            }
        }
    }

    static class AroundInterceptor implements MethodInterceptor {
        final List<String> log;
        AroundInterceptor(List<String> log) { this.log = log; }
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            log.add("Around-Start");
            Object result = invocation.proceed();
            log.add("Around-End");
            return result;
        }
    }

    static class BeforeInterceptor implements MethodInterceptor {
        final List<String> log;
        BeforeInterceptor(List<String> log) { this.log = log; }
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            log.add("Before");
            return invocation.proceed();
        }
    }

    static class AfterInterceptor implements MethodInterceptor {
        final List<String> log;
        AfterInterceptor(List<String> log) { this.log = log; }
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Object result = invocation.proceed();
            log.add("After");
            return result;
        }
    }

    static class SkipInterceptor implements MethodInterceptor {
        @Override public Object invoke(MethodInvocation invocation) throws Throwable { return 999; }
    }

    static class ModifyArgsInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            invocation.setArgs(new Object[]{10, 20});
            return invocation.proceed();
        }
    }

    static List<String> newLog() { return Collections.synchronizedList(new ArrayList<>()); }

    // ==================== 测试用例 ====================

    @Test
    void testSingleInterceptor() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new LoggingInterceptor("Log", log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of("Log-Before", "Log-After"), log);
    }

    @Test
    void testMultipleInterceptors() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new LoggingInterceptor("L1", log));
        pf.addInterceptor(m, new LoggingInterceptor("L2", log));
        pf.addInterceptor(m, new LoggingInterceptor("L3", log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of(
            "L1-Before", "L2-Before", "L3-Before", "L3-After", "L2-After", "L1-After"
        ), log);
    }

    @Test
    void testAroundInterceptor() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new AroundInterceptor(log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of("Around-Start", "Around-End"), log);
    }

    @Test
    void testBeforeAroundAfterOrder() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        // 注意：要得到 "Before, Around-Start, Around-End, After" 的执行顺序
        // 拦截器链必须是: Before → After → Around
        pf.addInterceptor(m, new BeforeInterceptor(log));
        pf.addInterceptor(m, new AfterInterceptor(log));
        pf.addInterceptor(m, new AroundInterceptor(log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of(
            "Before", "Around-Start", "Around-End", "After"
        ), log);
    }

    @Test
    void testAroundCanSkipTarget() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new SkipInterceptor());
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(999, proxy.add(2, 3));
    }

    @Test
    void testAroundCanModifyArgs() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new ModifyArgsInterceptor());
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(30, proxy.add(2, 3));
    }

    @Test
    void testMultipleMethods() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method addM = OrderServiceImpl.class.getMethod("add", int.class, int.class);
        Method mulM = OrderServiceImpl.class.getMethod("multiply", int.class, int.class);
        pf.addInterceptor(addM, new LoggingInterceptor("Add", log));
        pf.addInterceptor(mulM, new LoggingInterceptor("Mul", log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(6, proxy.multiply(2, 3));
        assertTrue(log.contains("Add-Before"));
        assertTrue(log.contains("Mul-Before"));
    }

    @Test
    void testCglibProxy() throws Throwable {
        PlainService target = new PlainService();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = PlainService.class.getMethod("add", int.class, int.class);
        pf.addInterceptor(m, new LoggingInterceptor("Log", log));
        
        PlainService proxy = (PlainService) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));
        assertEquals(List.of("Log-Before", "Log-After"), log);
    }

    @Test
    void testExceptionInTarget() throws Throwable {
        OrderServiceImpl target = new OrderServiceImpl();
        List<String> log = newLog();
        
        ProxyFactory pf = new ProxyFactory(target);
        Method m = OrderServiceImpl.class.getMethod("fail");
        pf.addInterceptor(m, new LoggingInterceptor("Log", log));
        
        BusinessService proxy = (BusinessService) pf.getProxy();
        try { proxy.fail(); fail("Should throw"); } catch (Throwable t) { /* expected */ }
        assertEquals(List.of("Log-Before", "Log-After"), log);
    }
}
