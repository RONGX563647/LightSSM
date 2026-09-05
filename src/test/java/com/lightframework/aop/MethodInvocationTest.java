package com.lightframework.aop;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MethodInvocationTest {

    static class Calc { public int add(int a, int b) { return a + b; } }

    static class OrderInterceptor implements MethodInterceptor {
        final String name;
        final List<String> log;
        OrderInterceptor(String n, List<String> l) { name = n; log = l; }
        @Override
        public Object invoke(MethodInvocation inv) throws Throwable {
            log.add(name + "-before");
            try { return inv.proceed(); } finally { log.add(name + "-after"); }
        }
    }

    static class ModifyInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation inv) throws Throwable {
            inv.setArgs(new Object[]{10, 20});
            return inv.proceed();
        }
    }

    static class SkipInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation inv) { return 999; }
    }

    private MethodInvocation build(Object target, Method m, Object[] args, List<MethodInterceptor> chain) {
        return new MethodInvocation(target, m, args, target, chain);
    }

    @Test
    void chainProceedOrder() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （MethodInvocation.obtain/release 对象池嵌套层数保护 + proceed 责任链推进，对应 MethodInvocation.java 练习）
        List<String> log = new ArrayList<>();
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3},
            List.of(new OrderInterceptor("A", log), new OrderInterceptor("B", log)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("A-before", "B-before", "B-after", "A-after"), log);
    }

    @Test
    void argsModificationPropagates() throws Throwable {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿
        // （MethodInvocation.setArgs 改写参数传到目标方法，对应 MethodInvocation.java 的 L1 练习）
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new ModifyInterceptor()));
        assertEquals(30, mi.proceed());
    }

    @Test
    void skipTarget() throws Throwable {
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new SkipInterceptor()));
        assertEquals(999, mi.proceed());
    }

    @Test
    void sequentialInvocationsIsolated() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （对象池每次 obtain 重置 currentInterceptorIndex，顺序调用不串数据）
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi1 = build(target, m, new Object[]{2, 3}, List.of());
        assertEquals(5, mi1.proceed());
        MethodInvocation mi2 = build(target, m, new Object[]{4, 5}, List.of());
        assertEquals(9, mi2.proceed());
    }
}
