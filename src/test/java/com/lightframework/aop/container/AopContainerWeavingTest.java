package com.lightframework.aop.container;

import com.lightframework.aop.annotation.Aspect;
import com.lightframework.aop.annotation.Before;
import com.lightframework.aop.annotation.Pointcut;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.DependsOn;
import com.lightframework.di.annotation.Service;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端验证：AspectJAutoProxyCreator 已通过 META-INF/lightssm.spi 接入 IoC 容器，
 * 在真实 AnnotationConfigApplicationContext 中，@Aspect 切面应当被自动织入目标 Bean。
 *
 * 关键约束说明（详见 docs/ARCHITECTURE.md 已知缺口 P0/P1）：
 * 1. AspectJAutoProxyCreator 必须作为 @Component 直接登记进 SPI，因为它实现的 BeanPostProcessor
 *    需要在 registerBeanPostProcessors() 阶段就被收集进 BPP 链；@Configuration 的 @Bean 形式因
 *    processBeanMethods() 在 preInstantiateSingletons() 之后运行而永远进不了 BPP 链。
 * 2. 切面 Bean 必须先于目标 Bean 完成创建与解析（parseAspect），否则目标 Bean 在 aspectInfos 为空时
 *    会被直接返回、不再织入。这里用 @DependsOn("loggingAspect") 保证顺序（顶层框架缺陷，见 ARCHITECTURE.md P1）。
 *
 * 本测试同时锁定"修复后行为"：@Before 通知在容器内真实执行、目标返回值不被破坏、返回对象是代理。
 */
public class AopContainerWeavingTest {

    public interface GreetingService {
        String hello(String name);
    }

    @Service
    @DependsOn("loggingAspect")
    public static class GreetingServiceImpl implements GreetingService {
        @Override
        public String hello(String name) {
            return "Hello, " + name;
        }
    }

    @Aspect
    @Component
    public static class LoggingAspect {
        public final List<String> events = new CopyOnWriteArrayList<>();

        @Pointcut("execution(* com.lightframework.aop.container.*.hello(..))")
        public void greetingPointcut() {}

        @Before("greetingPointcut()")
        public void logBefore() {
            events.add("before");
        }
    }

    @Test
    void aspectWeavesIntoTargetBeanInRealContainer() throws Exception {
        AnnotationConfigApplicationContext ctx =
                new AnnotationConfigApplicationContext(GreetingServiceImpl.class, LoggingAspect.class);

        GreetingService svc = ctx.getBean(GreetingService.class);
        assertNotNull(svc);

        // 1) 织入后返回的是代理（JDK 动态代理实现接口），不再是原始 impl
        assertNotSame(GreetingServiceImpl.class, svc.getClass());
        assertTrue(GreetingService.class.isAssignableFrom(svc.getClass()));

        // 2) 真实调用触发 @Before 通知，且目标返回值未被破坏
        String result = svc.hello("World");
        assertEquals("Hello, World", result);

        // 3) 验证切面在真实容器内被织入：@Before 通知已执行
        LoggingAspect aspect = ctx.getBean(LoggingAspect.class);
        assertEquals(1, aspect.events.size(), "期望 @Before 通知在容器内被织入执行一次");
        assertEquals("before", aspect.events.get(0));
    }
}
