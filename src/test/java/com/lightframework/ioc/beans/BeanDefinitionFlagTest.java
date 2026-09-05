package com.lightframework.ioc.beans;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 BeanDefinition 作用域标志 / 主候选 / 懒加载 / 限定符的当前正确行为。
 */
public class BeanDefinitionFlagTest {

    @Test
    void singletonAndPrototype() {
        // 验证 TODO[L3] 练习目标(setScope 策略模式)：singleton/prototype 标志正确
        BeanDefinition bd = new BeanDefinition();
        bd.setScope("singleton");
        assertTrue(bd.isSingleton());
        assertEquals("singleton", bd.getScope());

        bd.setScope("prototype");
        assertTrue(bd.isPrototype());
        assertEquals("prototype", bd.getScope());
    }

    @Test
    void customScope() {
        BeanDefinition bd = new BeanDefinition();
        bd.setScope("request");
        assertTrue(bd.isCustomScope());
        assertEquals("request", bd.getScope());
    }

    @Test
    void primaryLazyQualifier() {
        // 验证 TODO[L2] 练习目标(registerOrCollectBeanDefinition)：注解标志位正确写入 BeanDefinition
        BeanDefinition bd = new BeanDefinition("x", String.class);
        bd.setPrimary(true);
        bd.setLazyInit(true);
        bd.setQualifier("q");
        assertTrue(bd.isPrimary());
        assertTrue(bd.isLazyInit());
        assertEquals("q", bd.getQualifier());
    }

    @Test
    void equalsHashCodeContract() {
        // 验证 TODO[L1] 练习目标：用户实现 BeanDefinition.equals/hashCode 后，下面注释中的断言应成立。
        // 当前仅校验 getBeanName/getBeanClass；实现 equals 后可改为
        //   new BeanDefinition("a", A.class).equals(new BeanDefinition("a", A.class)) == true
        //   new BeanDefinition("a", A.class).equals(new BeanDefinition("b", A.class)) == false
        BeanDefinition a = new BeanDefinition("a", String.class);
        assertEquals("a", a.getBeanName());
        assertEquals(String.class, a.getBeanClass());
    }
}
