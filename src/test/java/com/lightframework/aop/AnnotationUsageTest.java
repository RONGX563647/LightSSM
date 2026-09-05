package com.lightframework.aop;

import com.lightframework.aop.annotation.After;
import com.lightframework.aop.annotation.AfterReturning;
import com.lightframework.aop.annotation.AfterThrowing;
import com.lightframework.aop.annotation.Around;
import com.lightframework.aop.annotation.Aspect;
import com.lightframework.aop.annotation.Before;
import com.lightframework.aop.annotation.Pointcut;
import com.lightframework.aop.core.ProceedingJoinPoint;
import com.lightframework.di.annotation.Component;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationUsageTest {

    @Aspect
    static class SampleAspect {
        @Pointcut("execution(* *(..))")
        void pc() {}
        @Before("pc()")
        public void before() {}
        @After("pc()")
        public void after() {}
        @Around("pc()")
        public Object around(ProceedingJoinPoint pjp) throws Throwable { return pjp.proceed(); }
        @AfterReturning(value = "pc()", returning = "r")
        public void afterReturning(Object r) {}
        @AfterThrowing(value = "pc()", throwing = "ex")
        public void afterThrowing(Throwable ex) {}
    }

    @Test
    void aspectIsComponentMetaAnnotated() {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（@Aspect 被 @Component 元注解修饰，IOC 可扫描）
        assertTrue(SampleAspect.class.isAnnotationPresent(Aspect.class));
        // @Component 是写在 @Aspect 定义上的元注解，因此在 @Aspect 注解类型本身上可直接查到：
        // 注意 SampleAspect 上 isn't directly @Component，而是经由 @Aspect 的元注解间接获得。
        assertTrue(com.lightframework.aop.annotation.Aspect.class.isAnnotationPresent(Component.class));
    }

    @Test
    void adviceAnnotationsTargetMethod() {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（@Before/@After/@Around 仅可用于 METHOD）
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, Before.class.getAnnotation(Target.class).value());
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, After.class.getAnnotation(Target.class).value());
        assertArrayEquals(new ElementType[]{ElementType.METHOD}, Around.class.getAnnotation(Target.class).value());
    }

    @Test
    void adviceValuesPresent() throws Throwable {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（注解 value/returning/throwing 元数据可读取）
        Method before = SampleAspect.class.getDeclaredMethod("before");
        assertEquals("pc()", before.getAnnotation(Before.class).value());
        Method ar = SampleAspect.class.getDeclaredMethod("afterReturning", Object.class);
        assertEquals("r", ar.getAnnotation(AfterReturning.class).returning());
        Method at = SampleAspect.class.getDeclaredMethod("afterThrowing", Throwable.class);
        assertEquals("ex", at.getAnnotation(AfterThrowing.class).throwing());
    }

    @Test
    void pointcutAnnotationCarriesValue() throws Throwable {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（@Pointcut 命名切点可被引用解析）
        Method pc = SampleAspect.class.getDeclaredMethod("pc");
        assertEquals("execution(* *(..))", pc.getAnnotation(Pointcut.class).value());
    }
}
