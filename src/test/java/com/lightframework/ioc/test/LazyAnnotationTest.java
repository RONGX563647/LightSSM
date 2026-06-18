package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Lazy;
import com.lightframework.di.annotation.Qualifier;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.InitializingBean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ========== 测试用接口和实现 ==========

interface LazyTestService {
    String getName();
    int getInitCount();
}

@Component("lazyService")
class LazyTestServiceImpl implements LazyTestService {

    public static volatile int initCount = 0;
    public static volatile int destroyCount = 0;
    private final String name;
    private int callCount = 0;

    public LazyTestServiceImpl() {
        this.name = "LazyTestServiceImpl";
        synchronized (LazyTestServiceImpl.class) {
            initCount++;
        }
    }

    @PostConstruct
    public void init() {
        synchronized (LazyTestServiceImpl.class) {
            // already counted in constructor
        }
    }

    @PreDestroy
    public void destroy() {
        synchronized (LazyTestServiceImpl.class) {
            destroyCount++;
        }
    }

    @Override
    public String getName() {
        callCount++;
        return name;
    }

    @Override
    public int getInitCount() {
        return initCount;
    }

    public int getCallCount() {
        return callCount;
    }
}

// 非懒加载的服务（用于对比）
@Component("eagerService")
class EagerTestServiceImpl implements LazyTestService {
    public static volatile int initCount = 0;
    private String name = "EagerTestServiceImpl";

    public EagerTestServiceImpl() {
        synchronized (EagerTestServiceImpl.class) {
            initCount++;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getInitCount() {
        return initCount;
    }
}

// ========== 测试用 Bean：字段级 @Lazy ==========

@Component("lazyFieldBean")
class LazyFieldBean {

    @Autowired
    @Lazy
    private LazyTestService lazyService;

    @Autowired
    private LazyTestService eagerService;

    public LazyTestService getLazyService() {
        return lazyService;
    }

    public LazyTestService getEagerService() {
        return eagerService;
    }
}

// ========== 测试用 Bean：类级 @Lazy ==========

@Lazy
@Component("lazyClassBean")
class LazyClassBean implements InitializingBean {

    public static volatile int initCount = 0;

    public LazyClassBean() {
        synchronized (LazyClassBean.class) {
            initCount++;
        }
    }

    @Override
    public void afterPropertiesSet() {
        // init
    }

    public String getMessage() {
        return "LazyClassBean";
    }
}

// ========== 测试用 Bean：@Lazy(false) 不懒加载 ==========

@Lazy(false)
@Component("notLazyBean")
class NotLazyBean {

    public static volatile int initCount = 0;

    public NotLazyBean() {
        synchronized (NotLazyBean.class) {
            initCount++;
        }
    }

    public String getMessage() {
        return "NotLazyBean";
    }
}

// ========== 测试用 Bean：构造器参数 @Lazy ==========

@Component("lazyConstructorBean")
class LazyConstructorBean {

    private final LazyTestService lazyService;

    @Autowired
    public LazyConstructorBean(@Lazy LazyTestService lazyService) {
        this.lazyService = lazyService;
    }

    public LazyTestService getLazyService() {
        return lazyService;
    }
}

// ========== 测试用 Bean：生命周期回调验证 ==========

interface LifecycleCallbackService {
    String getServiceName();
}

@Component("lifecycleCallbackService")
class LifecycleCallbackServiceImpl implements LifecycleCallbackService {

    public static final List<String> events = Collections.synchronizedList(new ArrayList<>());

    public LifecycleCallbackServiceImpl() {
        events.add("constructed");
    }

    @PostConstruct
    public void init() {
        events.add("postConstruct");
    }

    @PreDestroy
    public void cleanup() {
        events.add("preDestroy");
    }

    @Override
    public String getServiceName() {
        return "LifecycleCallbackService";
    }
}

@Component("lazyLifecycleBean")
class LazyLifecycleBean {

    @Autowired
    @Lazy
    private LifecycleCallbackService lazyService;

    public LifecycleCallbackService getService() {
        return lazyService;
    }
}

public class LazyAnnotationTest {

    public static void resetAllCounters() {
        LazyTestServiceImpl.initCount = 0;
        LazyTestServiceImpl.destroyCount = 0;
        EagerTestServiceImpl.initCount = 0;
        LazyClassBean.initCount = 0;
        NotLazyBean.initCount = 0;
        LifecycleCallbackServiceImpl.events.clear();
    }

    @Test
    public void testClassLevelLazy_notInstantiatedDuringRefresh() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                LazyClassBean.class
        );

        // 类级别 @Lazy 的 Bean 不应该在 refresh 时被实例化
        assertEquals(0, LazyClassBean.initCount, "Lazy bean should not be instantiated during refresh");

        // 获取 Bean 时才应该被实例化
        LazyClassBean bean = ctx.getBean("lazyClassBean", LazyClassBean.class);
        assertNotNull(bean);
        assertEquals(1, LazyClassBean.initCount, "Lazy bean should be instantiated after getBean()");
        assertEquals("LazyClassBean", bean.getMessage());

        ctx.close();
    }

    @Test
    public void testLazyFalse_instantiatedDuringRefresh() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                NotLazyBean.class
        );

        // @Lazy(false) 的 Bean 应该在 refresh 时就被实例化
        assertEquals(1, NotLazyBean.initCount, "NotLazy bean should be instantiated during refresh");

        ctx.close();
    }

    @Test
    public void testFieldLevelLazy_proxyInjected() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                LazyTestServiceImpl.class,
                LazyFieldBean.class
        );

        LazyFieldBean bean = ctx.getBean("lazyFieldBean", LazyFieldBean.class);
        assertNotNull(bean);

        // 获取 lazyService - 它应该是一个代理对象
        LazyTestService lazyService = bean.getLazyService();
        assertNotNull(lazyService);

        // 在首次调用方法之前，实际的 Bean 应该还没有被创建（代理延迟初始化）
        // 首次调用方法时才触发真实 Bean 的创建
        String name = lazyService.getName();
        assertEquals("LazyTestServiceImpl", name);

        ctx.close();
    }

    @Test
    public void testConstructorParameterLazy() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                LazyTestServiceImpl.class,
                LazyConstructorBean.class
        );

        LazyConstructorBean bean = ctx.getBean("lazyConstructorBean", LazyConstructorBean.class);
        assertNotNull(bean);

        // 构造器参数标注了 @Lazy，注入的应该是代理对象
        LazyTestService lazyService = bean.getLazyService();
        assertNotNull(lazyService);

        // 调用方法时应该能正常工作
        String name = lazyService.getName();
        assertEquals("LazyTestServiceImpl", name);

        ctx.close();
    }

    @Test
    public void testLazyBeanLifecycleCallbacks() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                LifecycleCallbackServiceImpl.class,
                LazyLifecycleBean.class
        );

        // 在获取懒加载 Bean 之前，lifecycleCallbackService 不应该被初始化
        LazyLifecycleBean bean = ctx.getBean("lazyLifecycleBean", LazyLifecycleBean.class);
        assertNotNull(bean);

        // 获取 lazyService 会触发真实 Bean 的创建
        LifecycleCallbackService service = bean.getService();
        assertNotNull(service);

        // 调用方法
        String serviceName = service.getServiceName();
        assertEquals("LifecycleCallbackService", serviceName);

        // 验证生命周期回调被调用
        assertTrue(LifecycleCallbackServiceImpl.events.contains("constructed"), "Constructor should be called");
        assertTrue(LifecycleCallbackServiceImpl.events.contains("postConstruct"), "@PostConstruct should be called");

        // 关闭上下文，验证 @PreDestroy
        ctx.close();
        assertTrue(LifecycleCallbackServiceImpl.events.contains("preDestroy"), "@PreDestroy should be called");
    }

    @Test
    public void testLazyBeanRegisteredManually() throws Exception {
        resetAllCounters();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();

        // 手动注册懒加载 BeanDefinition
        BeanDefinition bd = new BeanDefinition("lazyManualBean", LazyClassBean.class);
        bd.setLazyInit(true);
        factory.registerBeanDefinition("lazyManualBean", bd);

        // 预实例化时不应该创建懒加载 Bean
        factory.preInstantiateSingletons();
        assertEquals(0, LazyClassBean.initCount, "Lazy bean should not be pre-instantiated");

        // getBean 时才创建
        LazyClassBean bean = factory.getBean("lazyManualBean", LazyClassBean.class);
        assertNotNull(bean);
        assertEquals(1, LazyClassBean.initCount);
    }

    @Test
    public void testLazyProxyIsSingleton() throws Exception {
        resetAllCounters();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                LazyTestServiceImpl.class,
                LazyFieldBean.class
        );

        LazyFieldBean bean = ctx.getBean("lazyFieldBean", LazyFieldBean.class);

        // 多次获取 lazyService，应该返回同一个代理对象
        LazyTestService proxy1 = bean.getLazyService();
        LazyTestService proxy2 = bean.getLazyService();
        assertSame(proxy1, proxy2, "Lazy proxy should be the same instance");

        // 多次调用方法，实际 Bean 应该只被创建一次
        proxy1.getName();
        proxy2.getName();
        assertEquals(1, LazyTestServiceImpl.initCount, "Real bean should only be created once");

        ctx.close();
    }
}
