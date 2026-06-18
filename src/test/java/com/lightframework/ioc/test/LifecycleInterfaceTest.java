package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DisposableBean;
import com.lightframework.ioc.core.InitializingBean;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bean 实现 InitializingBean
 */
@Component
class InitializingBeanImpl implements InitializingBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        lifecycleEvents.add("afterPropertiesSet");
    }
}

/**
 * Bean 实现 DisposableBean
 */
@Component
class DisposableBeanImpl implements DisposableBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @Override
    public void destroy() throws Exception {
        lifecycleEvents.add("destroy");
    }
}

/**
 * Bean 同时使用 @PostConstruct 和 InitializingBean
 */
@Component
class PostConstructAndInitializingBean implements InitializingBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @PostConstruct
    public void init() {
        lifecycleEvents.add("postConstruct");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        lifecycleEvents.add("afterPropertiesSet");
    }
}

/**
 * Bean 同时使用 @PreDestroy 和 DisposableBean
 */
@Component
class PreDestroyAndDisposableBean implements DisposableBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @PreDestroy
    public void cleanup() {
        lifecycleEvents.add("preDestroy");
    }

    @Override
    public void destroy() throws Exception {
        lifecycleEvents.add("destroy");
    }
}

/**
 * 仅使用 @PostConstruct 的 Bean（用于初始化顺序测试）
 */
@Component
class PostConstructOnlyBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @PostConstruct
    public void init() {
        lifecycleEvents.add("postConstruct");
    }
}

/**
 * 仅使用 @PreDestroy 的 Bean（用于销毁顺序测试）
 */
@Component
class PreDestroyOnlyBean {

    public static List<String> lifecycleEvents = new ArrayList<>();

    @PreDestroy
    public void cleanup() {
        lifecycleEvents.add("preDestroy");
    }
}

public class LifecycleInterfaceTest {

    @Test
    public void testInitializingBeanCalled() throws Exception {
        InitializingBeanImpl.lifecycleEvents.clear();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("initializingBean", InitializingBeanImpl.class);
        factory.registerBeanDefinition("initializingBean", bd);

        InitializingBeanImpl bean = factory.getBean("initializingBean", InitializingBeanImpl.class);
        assertNotNull(bean);
        assertTrue(InitializingBeanImpl.lifecycleEvents.contains("afterPropertiesSet"));
    }

    @Test
    public void testDisposableBeanCalled() throws Exception {
        DisposableBeanImpl.lifecycleEvents.clear();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("disposableBean", DisposableBeanImpl.class);
        factory.registerBeanDefinition("disposableBean", bd);

        DisposableBeanImpl bean = factory.getBean("disposableBean", DisposableBeanImpl.class);
        assertNotNull(bean);

        factory.destroyBeans();
        assertTrue(DisposableBeanImpl.lifecycleEvents.contains("destroy"));
    }

    @Test
    public void testPostConstructBeforeAfterPropertiesSet() throws Exception {
        PostConstructAndInitializingBean.lifecycleEvents.clear();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("postConstructAndInitializingBean", PostConstructAndInitializingBean.class);
        factory.registerBeanDefinition("postConstructAndInitializingBean", bd);

        PostConstructAndInitializingBean bean = factory.getBean("postConstructAndInitializingBean", PostConstructAndInitializingBean.class);
        assertNotNull(bean);

        List<String> events = PostConstructAndInitializingBean.lifecycleEvents;
        assertTrue(events.contains("postConstruct"));
        assertTrue(events.contains("afterPropertiesSet"));
        // 验证 @PostConstruct 先于 afterPropertiesSet
        int postConstructIndex = events.indexOf("postConstruct");
        int afterPropertiesSetIndex = events.indexOf("afterPropertiesSet");
        assertTrue(postConstructIndex < afterPropertiesSetIndex,
                "@PostConstruct should be called before afterPropertiesSet");
    }

    @Test
    public void testPreDestroyBeforeDestroy() throws Exception {
        PreDestroyAndDisposableBean.lifecycleEvents.clear();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("preDestroyAndDisposableBean", PreDestroyAndDisposableBean.class);
        factory.registerBeanDefinition("preDestroyAndDisposableBean", bd);

        PreDestroyAndDisposableBean bean = factory.getBean("preDestroyAndDisposableBean", PreDestroyAndDisposableBean.class);
        assertNotNull(bean);

        factory.destroyBeans();

        List<String> events = PreDestroyAndDisposableBean.lifecycleEvents;
        assertTrue(events.contains("preDestroy"));
        assertTrue(events.contains("destroy"));
        // 验证 @PreDestroy 先于 destroy
        int preDestroyIndex = events.indexOf("preDestroy");
        int destroyIndex = events.indexOf("destroy");
        assertTrue(preDestroyIndex < destroyIndex,
                "@PreDestroy should be called before destroy");
    }
}
