package com.lightframework.ioc.scope;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scope 模块单元测试
 * 测试 ScopeRegistry、ApplicationScope 的核心功能
 * RequestScope 和 SessionScope 需要 Servlet 容器支持，使用模拟方式测试
 */
class ScopeTest {

    private ScopeRegistry scopeRegistry;

    @BeforeEach
    void setUp() {
        scopeRegistry = new ScopeRegistry();
    }

    @AfterEach
    void tearDown() {
        scopeRegistry.destroyAll();
    }

    // ==================== ScopeRegistry 测试 ====================

    @Test
    @DisplayName("初始化时应注册内置作用域")
    void testBuiltinScopesRegistered() {
        assertTrue(scopeRegistry.hasScope(ScopeRegistry.REQUEST));
        assertTrue(scopeRegistry.hasScope(ScopeRegistry.SESSION));
        assertTrue(scopeRegistry.hasScope(ScopeRegistry.APPLICATION));
        assertEquals(3, scopeRegistry.getScopeCount());
    }

    @Test
    @DisplayName("注册和获取自定义作用域")
    void testRegisterCustomScope() {
        Scope customScope = new MockScope("custom");
        scopeRegistry.registerScope("custom", customScope);

        assertTrue(scopeRegistry.hasScope("custom"));
        assertSame(customScope, scopeRegistry.getScope("custom"));
        assertEquals(4, scopeRegistry.getScopeCount());
    }

    @Test
    @DisplayName("替换已存在的作用域")
    void testReplaceExistingScope() {
        Scope customScope1 = new MockScope("custom1");
        Scope customScope2 = new MockScope("custom2");

        scopeRegistry.registerScope("custom", customScope1);
        scopeRegistry.registerScope("custom", customScope2);

        assertSame(customScope2, scopeRegistry.getScope("custom"));
        assertEquals(4, scopeRegistry.getScopeCount()); // 替换不增加数量
    }

    @Test
    @DisplayName("移除作用域")
    void testRemoveScope() {
        Scope customScope = new MockScope("custom");
        scopeRegistry.registerScope("custom", customScope);

        Scope removed = scopeRegistry.removeScope("custom");
        assertSame(customScope, removed);
        assertFalse(scopeRegistry.hasScope("custom"));
    }

    @Test
    @DisplayName("获取作用域名称集合")
    void testGetScopeNames() {
        var names = scopeRegistry.getScopeNames();
        assertTrue(names.contains(ScopeRegistry.REQUEST));
        assertTrue(names.contains(ScopeRegistry.SESSION));
        assertTrue(names.contains(ScopeRegistry.APPLICATION));
    }

    @Test
    @DisplayName("销毁自定义作用域")
    void testDestroyCustomScopes() {
        scopeRegistry.registerScope("custom1", new MockScope("custom1"));
        scopeRegistry.registerScope("custom2", new MockScope("custom2"));
        assertEquals(5, scopeRegistry.getScopeCount());

        scopeRegistry.destroyCustomScopes();

        // 内置作用域应保留
        assertEquals(3, scopeRegistry.getScopeCount());
        assertTrue(scopeRegistry.hasScope(ScopeRegistry.REQUEST));
    }

    @Test
    @DisplayName("获取应用级作用域")
    void testGetApplicationScope() {
        ApplicationScope appScope = scopeRegistry.getApplicationScope();
        assertNotNull(appScope);
        assertEquals("application", appScope.getConversationId());
    }

    // ==================== ApplicationScope 测试 ====================

    @Test
    @DisplayName("ApplicationScope - Bean 创建和获取")
    void testApplicationScopeGetBean() throws Exception {
        ApplicationScope appScope = new ApplicationScope();

        Object bean1 = appScope.get("testBean", () -> new Object());
        Object bean2 = appScope.get("testBean", () -> new Object());

        assertSame(bean1, bean2); // 应返回同一实例
    }

    @Test
    @DisplayName("ApplicationScope - 移除 Bean")
    void testApplicationScopeRemoveBean() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        Object bean = appScope.get("testBean", () -> new Object());

        Object removed = appScope.remove("testBean");
        assertSame(bean, removed);

        // After removal, getting with a new factory should create a new instance
        Object newBean = appScope.get("testBean", () -> new Object());
        assertNotNull(newBean);
    }

    @Test
    @DisplayName("ApplicationScope - 销毁回调")
    void testApplicationScopeDestructionCallback() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        List<String> callbacks = new ArrayList<>();

        appScope.registerDestructionCallback("testBean", () -> callbacks.add("callback1"));
        appScope.get("testBean", () -> new Object());

        appScope.remove("testBean");
        assertEquals(1, callbacks.size());
        assertEquals("callback1", callbacks.get(0));
    }

    @Test
    @DisplayName("ApplicationScope - 防止重复注册回调")
    void testApplicationScopePreventDuplicateCallbacks() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        List<String> callbacks = new ArrayList<>();

        appScope.registerDestructionCallback("testBean", () -> callbacks.add("first"));
        appScope.registerDestructionCallback("testBean", () -> callbacks.add("second"));

        appScope.get("testBean", () -> new Object());
        appScope.remove("testBean");

        assertEquals(1, callbacks.size());
        assertEquals("first", callbacks.get(0));
    }

    @Test
    @DisplayName("ApplicationScope - 销毁所有 Bean")
    void testApplicationScopeDestroyAll() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        List<String> callbacks = new ArrayList<>();

        appScope.get("bean1", () -> new Object());
        appScope.get("bean2", () -> new Object());
        appScope.registerDestructionCallback("bean1", () -> callbacks.add("bean1"));
        appScope.registerDestructionCallback("bean2", () -> callbacks.add("bean2"));

        appScope.destroyAll();

        assertEquals(2, callbacks.size());
        assertEquals(0, appScope.getBeanCache().size());
    }

    @Test
    @DisplayName("ApplicationScope - 获取 Bean 名称列表")
    void testApplicationScopeGetBeanNames() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        appScope.get("bean1", () -> new Object());
        appScope.get("bean2", () -> new Object());

        String[] names = appScope.getBeanNames();
        assertEquals(2, names.length);
        assertTrue(List.of(names).contains("bean1"));
        assertTrue(List.of(names).contains("bean2"));
    }

    // ==================== WebScopeManager 测试 ====================

    @Test
    @DisplayName("WebScopeManager 单例获取")
    void testWebScopeManagerSingleton() {
        WebScopeManager manager1 = WebScopeManager.getInstance();
        WebScopeManager manager2 = WebScopeManager.getInstance();

        assertSame(manager1, manager2);
    }

    @Test
    @DisplayName("WebScopeManager 获取注册表")
    void testWebScopeManagerGetRegistry() {
        WebScopeManager manager = WebScopeManager.getInstance();
        assertNotNull(manager.getScopeRegistry());
        assertTrue(manager.getScopeRegistry().hasScope(ScopeRegistry.REQUEST));
    }

    // ==================== Mock Scope ====================

    static class MockScope implements Scope {
        private final String name;

        MockScope(String name) {
            this.name = name;
        }

        @Override
        public Object get(String name, ObjectFactory<?> objectFactory) {
            try {
                return objectFactory.getObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object remove(String name) {
            return null;
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback) {
        }

        @Override
        public String[] getBeanNames() {
            return new String[0];
        }

        @Override
        public String getConversationId() {
            return "mock-" + name;
        }
    }
}
