package com.lightframework.ioc.context;

import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 AnnotationConfigApplicationContext 基于 @Component 注解注册并实例化 Bean 的行为（"某注解扫描/解析"）。
 */
public class AnnotationConfigTest {

    @Component
    public static class MyService {
        public String name() {
            return "myService";
        }
    }

    @Test
    void registerComponentViaContext() throws Exception {
        // 验证 TODO[L2] 练习目标：registerOrCollectBeanDefinition / registerComponent 从类读取注解填充 BeanDefinition
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyService.class);
        try {
            MyService svc = ctx.getBean(MyService.class);
            assertNotNull(svc);
            assertEquals("myService", svc.name());
        } finally {
            ctx.close();
        }
    }

    @Test
    void scopeAndLazyFlags() throws Exception {
        // 验证 TODO[L2] 练习目标：注册时正确解析 @Scope 等注解写入 BeanDefinition
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MyService.class);
        try {
            DefaultListableBeanFactory bf = ctx.getBeanFactory();
            BeanDefinition bd = bf.getBeanDefinition("myService");
            assertNotNull(bd);
            assertTrue(bd.isSingleton());
        } finally {
            ctx.close();
        }
    }
}
