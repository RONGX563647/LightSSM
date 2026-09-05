package com.lightframework.ioc.core;

import com.lightframework.ioc.beans.BeanDefinition;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 DefaultListableBeanFactory 的注册 / 获取 / 别名 / 按类型收集等核心行为。
 */
public class BeanFactoryBasicTest {

    public static class SimpleBean {
        public String hello() {
            return "hi";
        }
    }

    @Test
    void registerAndGet() throws Exception {
        // 验证 TODO[L2] 练习目标：getBeansOfType —— 注册两个同类型 Bean，getBeansOfType 返回 size==2
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerBeanDefinition("s1", new BeanDefinition("s1", SimpleBean.class));
        bf.registerBeanDefinition("s2", new BeanDefinition("s2", SimpleBean.class));

        SimpleBean b1 = bf.getBean("s1", SimpleBean.class);
        assertNotNull(b1);
        assertEquals(2, bf.getBeansOfType(SimpleBean.class).size());
        assertTrue(bf.containsBean("s1"));
        assertTrue(bf.containsBeanDefinition("s1"));
    }

    @Test
    void aliasResolution() throws Exception {
        // 验证 TODO[L2] 练习目标：resolveAlias 别名链解析 + getAliases
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerBeanDefinition("real", new BeanDefinition("real", SimpleBean.class));
        bf.registerAlias("real", "alias1");
        bf.registerAlias("alias1", "alias2");

        assertSame(bf.getBean("real"), bf.getBean("alias2"));
        assertTrue(Arrays.asList(bf.getAliases("real")).contains("alias1"));
    }

    @Test
    void getBeanByType() throws Exception {
        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        bf.registerBeanDefinition("s1", new BeanDefinition("s1", SimpleBean.class));
        SimpleBean b = bf.getBean(SimpleBean.class);
        assertNotNull(b);
    }
}
