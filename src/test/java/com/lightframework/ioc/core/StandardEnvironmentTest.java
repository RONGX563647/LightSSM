package com.lightframework.ioc.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 StandardEnvironment 的 profile 接受逻辑。
 */
public class StandardEnvironmentTest {

    @Test
    void acceptsProfiles() {
        // 验证 TODO[L1] 练习目标：acceptsProfiles 语义 —— 空数组返回 true；任一命中返回 true；都不命中返回 false
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("dev", "test");
        assertTrue(env.acceptsProfiles("dev", "prod"));
        assertFalse(env.acceptsProfiles("prod"));
        assertTrue(env.acceptsProfiles());
    }
}
