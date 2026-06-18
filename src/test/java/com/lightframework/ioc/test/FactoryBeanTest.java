package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.FactoryBean;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.exception.NoSuchBeanDefinitionException;
import org.junit.jupiter.api.Test;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FactoryBean mechanism
 */
public class FactoryBeanTest {

    // Test service class
    public static class UserService {
        private String name;
        private int initialized = 0;
        private boolean destroyed = false;

        public UserService() {}

        public UserService(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getInitialized() {
            return initialized;
        }

        public void setInitialized(int initialized) {
            this.initialized = initialized;
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public void setDestroyed(boolean destroyed) {
            this.destroyed = destroyed;
        }

        @PostConstruct
        public void init() {
            this.initialized = 1;
        }

        @PreDestroy
        public void cleanup() {
            this.destroyed = true;
        }
    }

    // Singleton FactoryBean
    public static class UserServiceFactoryBean implements FactoryBean<UserService> {
        private String name;
        private boolean factoryDestroyed = false;

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public UserService getObject() {
            return new UserService(name);
        }

        @Override
        public Class<?> getObjectType() {
            return UserService.class;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        @PreDestroy
        public void cleanup() {
            this.factoryDestroyed = true;
        }

        public boolean isFactoryDestroyed() {
            return factoryDestroyed;
        }
    }

    // Prototype FactoryBean
    public static class PrototypeUserServiceFactoryBean implements FactoryBean<UserService> {
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public UserService getObject() {
            return new UserService(name);
        }

        @Override
        public Class<?> getObjectType() {
            return UserService.class;
        }

        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    // Test consumer class that uses @Autowired with FactoryBean-created object
    @Component
    public static class UserController {
        @Autowired
        private UserService userService;

        public UserService getUserService() {
            return userService;
        }
    }

    @Test
    public void testFactoryBeanCreatesObject() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        // Register FactoryBean
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // Get the UserService created by FactoryBean
        UserService userService = factory.getBean("userServiceFactory", UserService.class);
        assertNotNull(userService);
        assertEquals(UserService.class, userService.getClass());
    }

    @Test
    public void testGetFactoryBeanItself() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // Get FactoryBean itself using "&" prefix
        Object factoryBean = factory.getBean("&userServiceFactory");
        assertNotNull(factoryBean);
        assertTrue(factoryBean instanceof FactoryBean);
        assertTrue(factoryBean instanceof UserServiceFactoryBean);
    }

    @Test
    public void testFactoryBeanSingleton() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // Get UserService twice - should return same instance
        UserService userService1 = factory.getBean("userServiceFactory", UserService.class);
        UserService userService2 = factory.getBean("userServiceFactory", UserService.class);

        assertSame(userService1, userService2);
    }

    @Test
    public void testFactoryBeanPrototype() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(PrototypeUserServiceFactoryBean.class);
        factory.registerBeanDefinition("prototypeUserServiceFactory", bd);

        // Get UserService twice - should return different instances
        UserService userService1 = factory.getBean("prototypeUserServiceFactory", UserService.class);
        UserService userService2 = factory.getBean("prototypeUserServiceFactory", UserService.class);

        assertNotSame(userService1, userService2);
    }

    @Test
    public void testFactoryBeanGetObjectType() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // getType should return the object type, not the FactoryBean type
        Class<?> type = factory.getType("userServiceFactory");
        assertEquals(UserService.class, type);

        // But for "&userServiceFactory", it should return the FactoryBean type
        Class<?> factoryType = factory.getType("&userServiceFactory");
        assertEquals(UserServiceFactoryBean.class, factoryType);
    }

    @Test
    public void testFactoryBeanLifecycleCallbacks() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // Get the UserService created by FactoryBean
        UserService userService = factory.getBean("userServiceFactory", UserService.class);

        // Verify @PostConstruct was called on the created object
        assertEquals(1, userService.getInitialized());

        // Get FactoryBean reference before destroying
        UserServiceFactoryBean factoryBean = factory.getBean("&userServiceFactory", UserServiceFactoryBean.class);

        // Destroy beans
        factory.destroyBeans();

        // Verify @PreDestroy was called on the FactoryBean
        assertTrue(factoryBean.isFactoryDestroyed());
    }

    @Test
    public void testFactoryBeanWithAutowired() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        // Register FactoryBean
        BeanDefinition factoryBd = new BeanDefinition();
        factoryBd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userService", factoryBd);

        // Register consumer
        BeanDefinition controllerBd = new BeanDefinition();
        controllerBd.setBeanClass(UserController.class);
        factory.registerBeanDefinition("userController", controllerBd);

        // Get the controller - it should have UserService injected
        UserController controller = factory.getBean("userController", UserController.class);
        assertNotNull(controller);
        assertNotNull(controller.getUserService());
        assertTrue(controller.getUserService() instanceof UserService);
    }

    @Test
    public void testFactoryBeanContainsBean() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("userServiceFactory", bd);

        // Both the factory bean and the "&" prefix should be recognized
        assertTrue(factory.containsBean("userServiceFactory"));
        assertTrue(factory.containsBean("&userServiceFactory"));
        assertFalse(factory.containsBean("nonExistentBean"));
    }

    @Test
    public void testFactoryBeanIsSingleton() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        BeanDefinition singletonBd = new BeanDefinition();
        singletonBd.setBeanClass(UserServiceFactoryBean.class);
        factory.registerBeanDefinition("singletonFactory", singletonBd);

        BeanDefinition prototypeBd = new BeanDefinition();
        prototypeBd.setBeanClass(PrototypeUserServiceFactoryBean.class);
        factory.registerBeanDefinition("prototypeFactory", prototypeBd);

        // The FactoryBean itself is a singleton (defined by BeanDefinition)
        assertTrue(factory.isSingleton("singletonFactory"));
        assertTrue(factory.isSingleton("&singletonFactory"));

        assertTrue(factory.isSingleton("prototypeFactory"));
    }
}
