package com.lightframework.ioc.scope;

import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 作用域集成测试
 * 测试 singleton、prototype、request、session、application 作用域的完整功能
 */
class BeanScopeIntegrationTest {

    private DefaultListableBeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        beanFactory = new DefaultListableBeanFactory();
    }

    @AfterEach
    void tearDown() {
        beanFactory.destroyBeans();
    }

    // ==================== BeanDefinition 作用域测试 ====================

    @Test
    @DisplayName("BeanDefinition 默认作用域为 singleton")
    void testDefaultScopeIsSingleton() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        assertEquals("singleton", bd.getScope());
        assertTrue(bd.isSingleton());
        assertFalse(bd.isPrototype());
        assertFalse(bd.isCustomScope());
    }

    @Test
    @DisplayName("BeanDefinition 设置 prototype 作用域")
    void testPrototypeScopeDefinition() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        bd.setScope("prototype");
        assertEquals("prototype", bd.getScope());
        assertFalse(bd.isSingleton());
        assertTrue(bd.isPrototype());
        assertFalse(bd.isCustomScope());
    }

    @Test
    @DisplayName("BeanDefinition 设置 request 自定义作用域")
    void testRequestCustomScope() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        bd.setScope("request");
        assertEquals("request", bd.getScope());
        assertFalse(bd.isSingleton());
        assertFalse(bd.isPrototype());
        assertTrue(bd.isCustomScope());
        assertEquals("request", bd.getScopeName());
    }

    @Test
    @DisplayName("BeanDefinition 设置 session 自定义作用域")
    void testSessionCustomScope() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        bd.setScope("session");
        assertEquals("session", bd.getScope());
        assertTrue(bd.isCustomScope());
        assertEquals("session", bd.getScopeName());
    }

    @Test
    @DisplayName("BeanDefinition 设置 application 自定义作用域")
    void testApplicationCustomScope() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        bd.setScope("application");
        assertEquals("application", bd.getScope());
        assertTrue(bd.isCustomScope());
        assertEquals("application", bd.getScopeName());
    }

    @Test
    @DisplayName("BeanDefinition 设置自定义作用域")
    void testCustomScopeDefinition() {
        BeanDefinition bd = new BeanDefinition("testBean", Object.class);
        bd.setScope("websocket");
        assertEquals("websocket", bd.getScope());
        assertTrue(bd.isCustomScope());
        assertEquals("websocket", bd.getScopeName());
    }

    // ==================== ScopeRegistry 测试 ====================

    @Test
    @DisplayName("ScopeRegistry 内置作用域注册")
    void testScopeRegistryBuiltInScopes() {
        ScopeRegistry registry = beanFactory.getScopeRegistry();
        assertNotNull(registry);
        assertTrue(registry.hasScope("request"));
        assertTrue(registry.hasScope("session"));
        assertTrue(registry.hasScope("application"));
        assertEquals(3, registry.getScopeCount());
    }

    @Test
    @DisplayName("注册自定义作用域")
    void testRegisterCustomScope() {
        ScopeRegistry registry = beanFactory.getScopeRegistry();
        com.lightframework.ioc.scope.Scope customScope = new MockScope();
        registry.registerScope("custom", customScope);
        
        assertTrue(registry.hasScope("custom"));
        assertSame(customScope, registry.getScope("custom"));
    }

    // ==================== ApplicationScope 集成测试 ====================

    @Test
    @DisplayName("ApplicationScope 单例语义")
    void testApplicationScopeSingletonBehavior() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        
        Object bean1 = appScope.get("testBean", () -> new Object());
        Object bean2 = appScope.get("testBean", () -> new Object());
        
        assertSame(bean1, bean2, "Application scope should return same instance");
    }

    @Test
    @DisplayName("ApplicationScope 销毁回调")
    void testApplicationScopeDestructionCallback() throws Exception {
        ApplicationScope appScope = new ApplicationScope();
        boolean[] callbackExecuted = {false};
        
        appScope.get("testBean", () -> new Object());
        appScope.registerDestructionCallback("testBean", () -> callbackExecuted[0] = true);
        
        appScope.remove("testBean");
        assertTrue(callbackExecuted[0], "Destruction callback should be executed");
    }

    // ==================== BeanFactory 自定义作用域集成测试 ====================

    @Test
    @DisplayName("BeanFactory 使用 ApplicationScope 创建 Bean")
    void testBeanFactoryWithApplicationScope() throws Exception {
        // 注册 application-scoped bean
        BeanDefinition bd = new BeanDefinition("appScopedBean", TestService.class);
        bd.setScope("application");
        beanFactory.registerBeanDefinition("appScopedBean", bd);
        
        // 获取 Bean
        TestService bean1 = beanFactory.getBean("appScopedBean", TestService.class);
        TestService bean2 = beanFactory.getBean("appScopedBean", TestService.class);
        
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertSame(bean1, bean2, "Application-scoped beans should be same instance");
    }

    @Test
    @DisplayName("BeanFactory singleton 作用域正常工作")
    void testSingletonScopeFactory() throws Exception {
        BeanDefinition bd = new BeanDefinition("singletonBean", TestService.class);
        beanFactory.registerBeanDefinition("singletonBean", bd);
        
        TestService bean1 = beanFactory.getBean("singletonBean", TestService.class);
        TestService bean2 = beanFactory.getBean("singletonBean", TestService.class);
        
        assertSame(bean1, bean2, "Singleton beans should be same instance");
    }

    @Test
    @DisplayName("BeanFactory prototype 作用域正常工作")
    void testPrototypeScopeFactory() throws Exception {
        BeanDefinition bd = new BeanDefinition("prototypeBean", TestService.class);
        bd.setScope("prototype");
        beanFactory.registerBeanDefinition("prototypeBean", bd);
        
        TestService bean1 = beanFactory.getBean("prototypeBean", TestService.class);
        TestService bean2 = beanFactory.getBean("prototypeBean", TestService.class);
        
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertNotSame(bean1, bean2, "Prototype beans should be different instances");
    }

    // ==================== Mock Scope ====================

    static class MockScope implements com.lightframework.ioc.scope.Scope {
        private final java.util.Map<String, Object> cache = new java.util.concurrent.ConcurrentHashMap<>();
        
        @Override
        public Object get(String name, com.lightframework.ioc.scope.Scope.ObjectFactory<?> objectFactory) {
            return cache.computeIfAbsent(name, k -> {
                try {
                    return objectFactory.getObject();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public Object remove(String name) {
            return cache.remove(name);
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback) {
        }

        @Override
        public String[] getBeanNames() {
            return cache.keySet().toArray(new String[0]);
        }

        @Override
        public String getConversationId() {
            return "mock-scope";
        }
    }

    // ==================== Test Beans ====================

    static class TestService {
        private String name;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
    }
}
