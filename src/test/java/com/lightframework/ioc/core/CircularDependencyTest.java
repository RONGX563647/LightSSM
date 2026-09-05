package com.lightframework.ioc.core;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.ioc.beans.BeanDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 DefaultListableBeanFactory 在字段注入场景下的循环依赖处理能力。
 */
public class CircularDependencyTest {

    public static class NodeA {
        @Autowired
        public NodeB b;
    }

    public static class NodeB {
        @Autowired
        public NodeA a;
    }

    @Test
    void fieldCircularDependencyResolves() throws Exception {
        // 验证 TODO[L3] 练习目标：字段注入下 A<->B 应成功实例化且互为同一实例（三级缓存暴露早期引用）
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerBeanDefinition("a", new BeanDefinition("a", NodeA.class));
        bf.registerBeanDefinition("b", new BeanDefinition("b", NodeB.class));

        NodeA a = bf.getBean("a", NodeA.class);
        NodeB b = bf.getBean("b", NodeB.class);
        assertNotNull(a.b);
        assertNotNull(b.a);
        assertSame(a, b.a);
        assertSame(b, a.b);
    }
}
