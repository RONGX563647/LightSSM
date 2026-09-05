package com.lightframework.ioc.core;

import com.lightframework.ioc.exception.BeanCurrentlyInCreationException;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 SingletonCache 三级缓存与循环依赖检测的当前正确行为。
 */
public class SingletonCacheTest {

    @Test
    void addSingletonAndGet() {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（addSingleton 写入一级缓存，getSingleton 直接返回同一实例）
        SingletonCache cache = new SingletonCache();
        Object bean = new Object();
        cache.addSingleton("a", bean);
        assertSame(bean, cache.getSingleton("a"));
        assertTrue(cache.isBeanCreated("a"));
    }

    @Test
    void threeLevelCacheFromFactory() {
        // 验证 TODO[L3] 练习目标：三级缓存查找 —— 处于创建中时 addSingletonFactory，getSingleton 能从三级缓存拿到早期对象并提升到二级
        SingletonCache cache = new SingletonCache();
        Object early = new Object();
        cache.beforeSingletonCreation("b");
        cache.addSingletonFactory("b", () -> early);
        assertSame(early, cache.getSingleton("b"));
        // 工厂已被提升到二级缓存，再次获取仍是同一早期对象
        assertSame(early, cache.getSingleton("b"));
        cache.afterSingletonCreation("b");
        assertFalse(cache.isSingletonCurrentlyInCreation("b"));
    }

    @Test
    void circularDependencyDetection() {
        // 验证 TODO[L2] 练习目标：beforeSingletonCreation 在重入同一 bean 时拼出调用链并抛循环依赖异常
        SingletonCache cache = new SingletonCache();
        cache.beforeSingletonCreation("a");
        cache.beforeSingletonCreation("b");
        BeanCurrentlyInCreationException ex = assertThrows(BeanCurrentlyInCreationException.class,
                () -> cache.beforeSingletonCreation("a"));
        assertTrue(ex.getMessage().contains("a"));
        cache.afterSingletonCreation("b");
        cache.afterSingletonCreation("a");
    }

    @Test
    void destroySingletonsInvokesAll() {
        // 验证 TODO[L1] 练习目标：逆序销毁 —— 注册 a,b 后每个都被回调（实现上应逆注册序执行）
        SingletonCache cache = new SingletonCache();
        cache.addSingleton("a", new Object());
        cache.addSingleton("b", new Object());
        Set<String> destroyed = new HashSet<>();
        cache.destroySingletons((name, bean) -> destroyed.add(name));
        assertEquals(Set.of("a", "b"), destroyed);
    }
}
