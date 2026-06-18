package com.lightframework.ioc.test;

import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.context.ApplicationContext;
import com.lightframework.ioc.core.*;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 实现 BeanNameAware
 */
class BeanNameAwareTestBean implements BeanNameAware {

    public static String capturedBeanName = null;

    @Override
    public void setBeanName(String name) {
        capturedBeanName = name;
    }
}

/**
 * Bean 实现 BeanFactoryAware
 */
class BeanFactoryAwareTestBean implements BeanFactoryAware {

    public static DefaultListableBeanFactory capturedBeanFactory = null;

    @Override
    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        capturedBeanFactory = beanFactory;
    }
}

/**
 * Bean 实现 ApplicationContextAware
 */
class ApplicationContextAwareTestBean implements ApplicationContextAware {

    public static ApplicationContext capturedApplicationContext = null;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        capturedApplicationContext = applicationContext;
    }
}

/**
 * Bean 实现所有 Aware 接口
 */
class AllAwareTestBean implements BeanNameAware, BeanFactoryAware, ApplicationContextAware {

    public static String capturedBeanName = null;
    public static DefaultListableBeanFactory capturedBeanFactory = null;
    public static ApplicationContext capturedApplicationContext = null;

    @Override
    public void setBeanName(String name) {
        capturedBeanName = name;
    }

    @Override
    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        capturedBeanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        capturedApplicationContext = applicationContext;
    }
}

/**
 * Bean 用于测试 Aware 调用顺序
 */
class AwareOrderTestBean implements BeanNameAware, BeanFactoryAware, ApplicationContextAware {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @Override
    public void setBeanName(String name) {
        lifecycleEvents.add("setBeanName:" + name);
    }

    @Override
    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        lifecycleEvents.add("setBeanFactory");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        lifecycleEvents.add("setApplicationContext");
    }

    @PostConstruct
    public void init() {
        lifecycleEvents.add("postConstruct");
    }
}

public class AwareInterfaceTest {

    @Test
    public void testBeanNameAware() throws Exception {
        BeanNameAwareTestBean.capturedBeanName = null;

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("myTestBean", BeanNameAwareTestBean.class);
        factory.registerBeanDefinition("myTestBean", bd);

        BeanNameAwareTestBean bean = factory.getBean("myTestBean", BeanNameAwareTestBean.class);
        assertNotNull(bean);
        assertEquals("myTestBean", BeanNameAwareTestBean.capturedBeanName);
    }

    @Test
    public void testBeanFactoryAware() throws Exception {
        BeanFactoryAwareTestBean.capturedBeanFactory = null;

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("factoryAwareBean", BeanFactoryAwareTestBean.class);
        factory.registerBeanDefinition("factoryAwareBean", bd);

        BeanFactoryAwareTestBean bean = factory.getBean("factoryAwareBean", BeanFactoryAwareTestBean.class);
        assertNotNull(bean);
        assertNotNull(BeanFactoryAwareTestBean.capturedBeanFactory);
        assertSame(factory, BeanFactoryAwareTestBean.capturedBeanFactory);
    }

    @Test
    public void testApplicationContextAware() throws Exception {
        ApplicationContextAwareTestBean.capturedApplicationContext = null;

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.setApplicationContext(new MockApplicationContext());
        BeanDefinition bd = new BeanDefinition("contextAwareBean", ApplicationContextAwareTestBean.class);
        factory.registerBeanDefinition("contextAwareBean", bd);

        ApplicationContextAwareTestBean bean = factory.getBean("contextAwareBean", ApplicationContextAwareTestBean.class);
        assertNotNull(bean);
        assertNotNull(ApplicationContextAwareTestBean.capturedApplicationContext);
    }

    @Test
    public void testMultipleAwareInterfaces() throws Exception {
        AllAwareTestBean.capturedBeanName = null;
        AllAwareTestBean.capturedBeanFactory = null;
        AllAwareTestBean.capturedApplicationContext = null;

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.setApplicationContext(new MockApplicationContext());
        BeanDefinition bd = new BeanDefinition("allAwareBean", AllAwareTestBean.class);
        factory.registerBeanDefinition("allAwareBean", bd);

        AllAwareTestBean bean = factory.getBean("allAwareBean", AllAwareTestBean.class);
        assertNotNull(bean);
        assertEquals("allAwareBean", AllAwareTestBean.capturedBeanName);
        assertNotNull(AllAwareTestBean.capturedBeanFactory);
        assertSame(factory, AllAwareTestBean.capturedBeanFactory);
        assertNotNull(AllAwareTestBean.capturedApplicationContext);
    }

    @Test
    public void testAwareOrder() throws Exception {
        AwareOrderTestBean.lifecycleEvents.clear();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.setApplicationContext(new MockApplicationContext());
        BeanDefinition bd = new BeanDefinition("orderBean", AwareOrderTestBean.class);
        factory.registerBeanDefinition("orderBean", bd);

        factory.getBean("orderBean", AwareOrderTestBean.class);

        List<String> events = AwareOrderTestBean.lifecycleEvents;
        assertEquals(4, events.size(), "Expected 4 lifecycle events");

        // 验证 Aware 调用顺序：BeanNameAware -> BeanFactoryAware -> ApplicationContextAware -> @PostConstruct
        assertTrue(events.get(0).startsWith("setBeanName:"), "BeanNameAware should be first");
        assertEquals("setBeanFactory", events.get(1), "BeanFactoryAware should be second");
        assertEquals("setApplicationContext", events.get(2), "ApplicationContextAware should be third");
        assertEquals("postConstruct", events.get(3), "@PostConstruct should be last");
    }

    @Test
    public void testApplicationContextAwareWithAnnotationContext() throws Exception {
        ApplicationContextAwareTestBean.capturedApplicationContext = null;

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                com.lightframework.ioc.test.AwareContextTestBean.class);

        assertNotNull(ApplicationContextAwareTestBean.capturedApplicationContext);
        assertSame(context, ApplicationContextAwareTestBean.capturedApplicationContext);

        context.close();
    }

    @Test
    public void testAllAwareWithAnnotationContext() throws Exception {
        AllAwareTestBean.capturedBeanName = null;
        AllAwareTestBean.capturedBeanFactory = null;
        AllAwareTestBean.capturedApplicationContext = null;

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                com.lightframework.ioc.test.AwareContextAllAwareBean.class);

        assertEquals("awareContextAllAwareBean", AllAwareTestBean.capturedBeanName);
        assertNotNull(AllAwareTestBean.capturedBeanFactory);
        assertNotNull(AllAwareTestBean.capturedApplicationContext);

        context.close();
    }
}

/**
 * Mock ApplicationContext for testing without full context
 */
class MockApplicationContext implements ApplicationContext {

    @Override
    public String getId() {
        return "mock-context";
    }

    @Override
    public String getApplicationName() {
        return "mock-app";
    }

    @Override
    public String getDisplayName() {
        return "Mock Context";
    }

    @Override
    public long getStartupDate() {
        return System.currentTimeMillis();
    }

    @Override
    public ApplicationContext getParent() {
        return null;
    }

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public Object getBean(String name) {
        return null;
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) {
        return null;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) {
        return null;
    }

    @Override
    public boolean containsBean(String name) {
        return false;
    }

    @Override
    public boolean isSingleton(String name) {
        return false;
    }

    @Override
    public boolean isPrototype(String name) {
        return false;
    }

    @Override
    public Class<?> getType(String name) {
        return null;
    }

    @Override
    public String[] getAliases(String name) {
        return new String[0];
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return false;
    }

    @Override
    public int getBeanDefinitionCount() {
        return 0;
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return new String[0];
    }

    @Override
    public <T> List<T> getBeansOfType(Class<T> type) {
        return new ArrayList<>();
    }

    @Override
    public <T> Map<String, T> getBeansOfTypeAsMap(Class<T> type) {
        return new HashMap<>();
    }

    @Override
    public <T> T getPrimaryBean(Class<T> type) {
        return null;
    }
}

/**
 * Test bean using ApplicationContextAware with full annotation context
 */
@Component("awareContextTestBean")
class AwareContextTestBean implements ApplicationContextAware {

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ApplicationContextAwareTestBean.capturedApplicationContext = applicationContext;
    }
}

/**
 * Test bean using all Aware interfaces with full annotation context
 */
@Component
class AwareContextAllAwareBean implements BeanNameAware, BeanFactoryAware, ApplicationContextAware {

    @Override
    public void setBeanName(String name) {
        AllAwareTestBean.capturedBeanName = name;
    }

    @Override
    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        AllAwareTestBean.capturedBeanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        AllAwareTestBean.capturedApplicationContext = applicationContext;
    }
}
