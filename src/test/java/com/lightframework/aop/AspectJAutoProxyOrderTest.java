package com.lightframework.aop;

import com.lightframework.aop.core.JoinPoint;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.aop.core.ProceedingJoinPoint;
import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.aop.interceptor.AfterMethodInterceptor;
import com.lightframework.aop.interceptor.AfterReturningMethodInterceptor;
import com.lightframework.aop.interceptor.AfterThrowingMethodInterceptor;
import com.lightframework.aop.interceptor.AroundMethodInterceptor;
import com.lightframework.aop.interceptor.BeforeMethodInterceptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 本测试"验证当前实际行为"并标注已知 bug，供 TODO[L3] 练习修复后作为回归目标。
 *
 * 已知 bug（见 AspectJAutoProxyCreator.buildInterceptorMap / getOrder）：
 *   AspectJAutoProxyCreator.getOrder() 按拦截器类名前缀排序，把 @Around 排到最后(ORDER_AROUND=20)，
 *   且完全忽略切面 @Order；导致最终织入链大致为 [Before, After, AfterReturning, AfterThrowing, Around]。
 *   这违反 Spring 语义：@Around 应最外层、@After 应最后(innermost finally)。
 *
 * 下面手动按 getOrder 的排序结果构建链（[Before, After, AfterReturning, AfterThrowing, Around]），
 * 复现并锁定当前实际执行顺序；修复 TODO[L3] 后，期望顺序应变为：
 *   [Around-start, Before, 目标, AfterReturning, After, Around-end]
 *   （@Around 最外层包裹一切；@After 在 @AfterReturning 之后执行）。
 */
public class AspectJAutoProxyOrderTest {

    // 使用接口类型作为目标，使 ProxyFactory 走 JDK 代理（避免依赖 CGLIB 运行时）；
    // 这样本测试在任意 JDK 上都能运行并锁定"当前实际(有 bug)的织入顺序"。
    interface Op { int add(int a, int b); }

    static class Calc implements Op { public int add(int a, int b) { return a + b; } }

    // public static + 通知方法返回 Object：原因同 AdviceInterceptorTest（Lookup 访问与 invokeExact 返回类型）。
    public static class Aspect {
        final List<String> log = new ArrayList<>();
        public Object before(JoinPoint jp) { log.add("Before"); return null; }
        public Object after(JoinPoint jp) { log.add("After"); return null; }
        public Object afterReturning(JoinPoint jp) { log.add("AfterReturning"); return null; }
        public Object afterThrowing(JoinPoint jp) { log.add("AfterThrowing"); return null; }
        public Object around(ProceedingJoinPoint pjp) throws Throwable {
            log.add("Around-start");
            Object r = pjp.proceed();
            log.add("Around-end");
            return r;
        }
    }

    @Test
    void currentOrderDocumentsBug() throws Throwable {
        // 验证 TODO[L3] 练习目标：用户手写实现后运行本测试应全绿
        // （TODO[L3] 练习要求把期望顺序改为 [Around-start, Before, 目标, AfterReturning, After, Around-end]；
        //  此处先锁定"当前实际行为"作为回归基线——修复后本断言应改为期望顺序。）
        Aspect a = new Aspect();
        Method beforeM = Aspect.class.getMethod("before", JoinPoint.class);
        Method afterM = Aspect.class.getMethod("after", JoinPoint.class);
        Method arM = Aspect.class.getMethod("afterReturning", JoinPoint.class);
        Method atM = Aspect.class.getMethod("afterThrowing", JoinPoint.class);
        Method aroundM = Aspect.class.getMethod("around", ProceedingJoinPoint.class);

        // 复现 getOrder() 的排序结果：Before(0) < After(10) < AfterReturning(11) < AfterThrowing(12) < Around(20)
        List<MethodInterceptor> chain = new ArrayList<>();
        chain.add(new BeforeMethodInterceptor(beforeM, a));
        chain.add(new AfterMethodInterceptor(afterM, a));
        chain.add(new AfterReturningMethodInterceptor(arM, a));
        chain.add(new AfterThrowingMethodInterceptor(atM, a));
        chain.add(new AroundMethodInterceptor(aroundM, a));

        Calc target = new Calc();
        Method m = Calc.class.getMethod("add", int.class, int.class);
        ProxyFactory pf = new ProxyFactory(target);
        pf.addInterceptors(m, chain);

        Op proxy = (Op) pf.getProxy();
        assertEquals(5, proxy.add(2, 3));

        // 当前实际（buggy）顺序：@Around 不在最外层、@After 跑在 @AfterReturning 之前
        assertEquals(List.of("Before", "Around-start", "Around-end", "AfterReturning", "After"), a.log);
    }
}
