package com.lightframework.ioc.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 PropertyPlaceholderConfigurer 的 ${key} / ${key:default} 占位符解析行为。
 */
public class PropertyPlaceholderConfigurerTest {

    @Test
    void resolveWithDefault() {
        // 验证 TODO[L2] 练习目标：手写 ${key:default} 解析 —— 加载属性后用占位符取值，缺失则用默认值
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        System.setProperty("lightssm.ioc.test.app.name", "foo");
        try {
            assertEquals("foo", ppc.resolvePlaceholder("${lightssm.ioc.test.app.name}"));
            assertEquals("foo", ppc.resolvePlaceholder("${lightssm.ioc.test.app.name:bar}"));
            assertEquals("bar", ppc.resolvePlaceholder("${lightssm.ioc.test.missing:bar}"));
        } finally {
            System.clearProperty("lightssm.ioc.test.app.name");
        }
    }

    @Test
    void unresolvedThrows() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        assertThrows(IllegalArgumentException.class,
                () -> ppc.resolvePlaceholder("${lightssm.ioc.test.no.such.key}"));
    }
}
