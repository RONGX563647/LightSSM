package com.lightframework.aop;

import com.lightframework.aop.core.JoinPoint;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProceedingJoinPoint;
import com.lightframework.aop.interceptor.AfterMethodInterceptor;
import com.lightframework.aop.interceptor.AfterReturningMethodInterceptor;
import com.lightframework.aop.interceptor.AfterThrowingMethodInterceptor;
import com.lightframework.aop.interceptor.AroundMethodInterceptor;
import com.lightframework.aop.interceptor.BeforeMethodInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AdviceInterceptorTest {

    static class Calc {
        public int add(int a, int b) { return a + b; }
        public int fail() { throw new RuntimeException("boom"); }
    }

    // 必须为 public static：通知拦截器内部用 MethodHandles.lookup().unreflect() 反射通知方法，
    // 若 Aspect 为包私有嵌套类，lookup 所在包(aop.interceptor)无权访问其方法会抛 IllegalAccessException。
    // 通知方法返回 Object（而非 void）：框架拦截器对适配后的 MethodHandle 用 invokeExact 调用，
    // 要求调用点返回类型为 Object；void 通知会被 invokeExact 判型失败（见各拦截器 TODO，可作为练习修复点）。
    public static class Aspect {
        final List<String> log = new ArrayList<>();
        public Object before(JoinPoint jp) { log.add("before:" + jp.getMethodName()); return null; }
        public Object beforeNoArg() { log.add("beforeNoArg"); return null; }
        public Object after(JoinPoint jp) { log.add("after"); return null; }
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            log.add("around-start");
            Object r = pjp.proceed();
            log.add("around-end");
            return r;
        }
        public Object aroundSkip(ProceedingJoinPoint pjp) { log.add("around-skip"); return 999; }
        public Object afterReturning(JoinPoint jp) { log.add("afterReturning"); return null; }
        public Object afterThrowing(JoinPoint jp) { log.add("afterThrowing"); return null; }
    }

    private MethodInvocation build(Object target, Method m, Object[] args, List<MethodInterceptor> chain) {
        // 用对象池 obtain 并提供目标 MethodHandle，使目标方法经 MethodHandle 直接调用
        // （否则回退到反射 method.invoke 会把目标异常包成 InvocationTargetException）。
        MethodHandle targetHandle;
        try {
            targetHandle = MethodHandles.lookup().unreflect(m).bindTo(target);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return MethodInvocation.obtain(target, m, args, target,
            chain.toArray(new MethodInterceptor[0]), targetHandle);
    }

    @Test
    void beforeRunsBeforeTarget() throws Throwable {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（BeforeMethodInterceptor 构造 MethodHandle 适配）
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（BeforeMethodInterceptor.invoke 链推进）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("before", JoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new BeforeMethodInterceptor(aspectM, a)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("before:add"), a.log);
    }

    @Test
    void beforeNoArgRuns() throws Throwable {
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("beforeNoArg");
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new BeforeMethodInterceptor(aspectM, a)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("beforeNoArg"), a.log);
    }

    @Test
    void afterRunsInFinally() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AfterMethodInterceptor.invoke finally 语义）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("after", JoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new AfterMethodInterceptor(aspectM, a)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("after"), a.log);
    }

    @Test
    void afterRunsEvenOnException() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（目标异常时 @After 仍执行且异常向上传播）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("after", JoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("fail");
        MethodInvocation mi = build(target, m, new Object[]{}, List.of(new AfterMethodInterceptor(aspectM, a)));
        assertThrows(RuntimeException.class, () -> mi.proceed());
        assertEquals(List.of("after"), a.log);
    }

    @Test
    void aroundControlsTarget() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AroundMethodInterceptor.invoke 包装 ProceedingJoinPoint）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("around", ProceedingJoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new AroundMethodInterceptor(aspectM, a)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("around-start", "around-end"), a.log);
    }

    @Test
    void aroundCanSkipTarget() throws Throwable {
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("aroundSkip", ProceedingJoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new AroundMethodInterceptor(aspectM, a)));
        assertEquals(999, mi.proceed());
        assertEquals(List.of("around-skip"), a.log);
    }

    @Test
    void afterReturningRuns() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （AfterReturningMethodInterceptor.invoke；注意 returning 属性暂未回传到通知参数，对应 L2 练习）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("afterReturning", JoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        MethodInvocation mi = build(target, m, new Object[]{2, 3}, List.of(new AfterReturningMethodInterceptor(aspectM, a)));
        assertEquals(5, mi.proceed());
        assertEquals(List.of("afterReturning"), a.log);
    }

    @Test
    void afterThrowingRunsAndRethrows() throws Throwable {
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
        // （AfterThrowingMethodInterceptor.invoke 捕获后重抛原异常）
        Aspect a = new Aspect();
        Method aspectM = Aspect.class.getMethod("afterThrowing", JoinPoint.class);
        Calc target = new Calc();
        Method m = Calc.class.getMethod("fail");
        MethodInvocation mi = build(target, m, new Object[]{}, List.of(new AfterThrowingMethodInterceptor(aspectM, a)));
        assertThrows(RuntimeException.class, () -> mi.proceed());
        assertEquals(List.of("afterThrowing"), a.log);
    }
}
