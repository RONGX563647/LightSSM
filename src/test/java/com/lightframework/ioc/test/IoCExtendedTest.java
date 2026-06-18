package com.lightframework.ioc.test;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.Primary;
import com.lightframework.ioc.annotation.Scope;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// ==================== 循环依赖测试类 ====================
// 双向循环依赖: A2 <-> B2
@Component
class CircularServiceA2 {
    @Autowired
    private CircularServiceB2 b;

    public CircularServiceB2 getB() {
        return b;
    }
}

@Component
class CircularServiceB2 {
    @Autowired
    private CircularServiceA2 a;

    public CircularServiceA2 getA() {
        return a;
    }
}

// 三级循环依赖 A3 -> B3 -> C3 -> A3
@Component
class CircularServiceA3 {
    @Autowired
    private CircularServiceB3 b;

    public CircularServiceB3 getB() {
        return b;
    }
}

@Component
class CircularServiceB3 {
    @Autowired
    private CircularServiceC3 c;

    public CircularServiceC3 getC() {
        return c;
    }
}

@Component
class CircularServiceC3 {
    @Autowired
    private CircularServiceA3 a;

    public CircularServiceA3 getA() {
        return a;
    }
}

// 自引用测试
@Component
class SelfReferencingBean {
    @Autowired(required = false)
    private SelfReferencingBean self;

    public SelfReferencingBean getSelf() {
        return self;
    }
}

// ==================== 继承测试类 ====================
class BaseService {
    @Autowired
    protected CircularServiceA2 baseDependency;

    public CircularServiceA2 getBaseDependency() {
        return baseDependency;
    }
}

@Component
class InheritedBean extends BaseService {
    @Autowired
    private CircularServiceB2 ownDependency;

    public CircularServiceB2 getOwnDependency() {
        return ownDependency;
    }
}

// ==================== @Primary 测试类 ====================
interface MessageService {
    String getMessage();
}

@Primary
@Component
class PrimaryMessageService implements MessageService {
    @Override
    public String getMessage() {
        return "primary";
    }
}

@Component
class SecondaryMessageService implements MessageService {
    @Override
    public String getMessage() {
        return "secondary";
    }
}

@Component
class ConsumerWithPrimary {
    @Autowired
    private MessageService messageService;

    public MessageService getMessageService() {
        return messageService;
    }
}

// ==================== @PostConstruct/@PreDestroy 测试类 ====================
@Component
class LifecycleBean {
    private boolean initCalled = false;
    private boolean destroyCalled = false;
    private int initCallCount = 0;

    @PostConstruct
    public void init1() {
        initCalled = true;
        initCallCount++;
    }

    @PostConstruct
    public void init2() {
        initCallCount++;
    }

    @PreDestroy
    public void cleanup1() {
        destroyCalled = true;
    }

    public boolean isInitCalled() {
        return initCalled;
    }

    public boolean isDestroyCalled() {
        return destroyCalled;
    }

    public int getInitCallCount() {
        return initCallCount;
    }
}

// ==================== 泛型注入测试类 ====================
@Component
class GenericBeanA {
    public String getName() { return "A"; }
}

@Component
class GenericBeanB {
    public String getName() { return "B"; }
}

@Component
class GenericConsumer {
    @Autowired
    private List<GenericBeanA> beanAList;

    public List<GenericBeanA> getBeanAList() {
        return beanAList;
    }
}

// ==================== Setter 注入测试类 ====================
@Component
class SetterInjectionBean {
    private CircularServiceA2 service;

    @Autowired
    public void setService(CircularServiceA2 service) {
        this.service = service;
    }

    public CircularServiceA2 getService() {
        return service;
    }
}

// ==================== 别名测试 ====================

public class IoCExtendedTest {

    @Test
    public void testCircularDependencyTwoWay() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            CircularServiceA2.class, CircularServiceB2.class);
        CircularServiceA2 a = ctx.getBean(CircularServiceA2.class);
        assertNotNull(a);
        assertNotNull(a.getB());
        assertSame(a, a.getB().getA());
        ctx.close();
    }

    @Test
    public void testAlias() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("originalBean", IoCTest.SimpleBean.class);
        factory.registerBeanDefinition("originalBean", bd);
        
        factory.registerAlias("originalBean", "alias1");
        factory.registerAlias("originalBean", "alias2");
        
        // 别名应该解析到同一个 bean
        assertTrue(factory.containsBean("alias1"));
        assertTrue(factory.containsBean("alias2"));
        
        String[] aliases = factory.getAliases("originalBean");
        assertEquals(2, aliases.length);
        
        // 通过别名获取 bean 应该返回同一个实例
        try {
            IoCTest.SimpleBean original = factory.getBean("originalBean", IoCTest.SimpleBean.class);
            IoCTest.SimpleBean viaAlias = factory.getBean("alias1", IoCTest.SimpleBean.class);
            assertSame(original, viaAlias);
        } catch (Exception e) {
            fail("Alias resolution failed: " + e.getMessage());
        }
    }

    @Test
    public void testInheritedFieldInjection() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            CircularServiceA2.class, CircularServiceB2.class, InheritedBean.class);
        InheritedBean bean = ctx.getBean(InheritedBean.class);
        
        // 验证父类字段注入
        assertNotNull(bean.getBaseDependency(), "Parent class @Autowired field should be injected");
        
        // 验证子类字段注入
        assertNotNull(bean.getOwnDependency(), "Own class @Autowired field should be injected");
        
        ctx.close();
    }

    @Test
    public void testLifecycleCallbacks() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        BeanDefinition bd = new BeanDefinition("lifecycleBean", LifecycleBean.class);
        factory.registerBeanDefinition("lifecycleBean", bd);
        
        LifecycleBean bean = factory.getBean("lifecycleBean", LifecycleBean.class);
        
        // @PostConstruct 应该被调用
        assertTrue(bean.isInitCalled(), "@PostConstruct should be called");
        assertEquals(2, bean.getInitCallCount(), "Multiple @PostConstruct methods should all be called");
        
        // 销毁时 @PreDestroy 应该被调用
        factory.destroyBeans();
        assertTrue(bean.isDestroyCalled(), "@PreDestroy should be called on destroy");
    }

    @Test
    public void testPrimaryBean() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd1 = new BeanDefinition("primaryService", PrimaryMessageService.class);
        bd1.setPrimary(true);
        factory.registerBeanDefinition("primaryService", bd1);
        
        BeanDefinition bd2 = new BeanDefinition("secondaryService", SecondaryMessageService.class);
        factory.registerBeanDefinition("secondaryService", bd2);
        
        BeanDefinition bd3 = new BeanDefinition("consumer", ConsumerWithPrimary.class);
        factory.registerBeanDefinition("consumer", bd3);
        
        ConsumerWithPrimary consumer = factory.getBean("consumer", ConsumerWithPrimary.class);
        
        // 应该注入 @Primary 标注的 bean
        assertNotNull(consumer.getMessageService());
        assertEquals("primary", consumer.getMessageService().getMessage());
    }

    @Test
    public void testPrimaryBeanAnnotationScanning() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            PrimaryMessageService.class, SecondaryMessageService.class, ConsumerWithPrimary.class);

        ConsumerWithPrimary consumer = ctx.getBean("consumerWithPrimary", ConsumerWithPrimary.class);

        // 通过注解扫描，@Primary 自动检测并注入 PrimaryMessageService
        assertNotNull(consumer.getMessageService());
        assertEquals("primary", consumer.getMessageService().getMessage());

        ctx.close();
    }

    @Test
    public void testGenericListInjection() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd1 = new BeanDefinition("genericBeanA1", GenericBeanA.class);
        factory.registerBeanDefinition("genericBeanA1", bd1);
        
        BeanDefinition bd2 = new BeanDefinition("genericBeanB", GenericBeanB.class);
        factory.registerBeanDefinition("genericBeanB", bd2);
        
        BeanDefinition bd3 = new BeanDefinition("genericConsumer", GenericConsumer.class);
        factory.registerBeanDefinition("genericConsumer", bd3);
        
        GenericConsumer consumer = factory.getBean("genericConsumer", GenericConsumer.class);
        
        // List<GenericBeanA> 应该只包含 GenericBeanA 类型的 bean
        assertNotNull(consumer.getBeanAList());
        assertEquals(1, consumer.getBeanAList().size());
        assertEquals("A", consumer.getBeanAList().get(0).getName());
    }

    @Test
    public void testSetterInjection() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd1 = new BeanDefinition("circularServiceA2", CircularServiceA2.class);
        factory.registerBeanDefinition("circularServiceA2", bd1);
        
        BeanDefinition bd2 = new BeanDefinition("circularServiceB2", CircularServiceB2.class);
        factory.registerBeanDefinition("circularServiceB2", bd2);
        
        BeanDefinition bd3 = new BeanDefinition("setterBean", SetterInjectionBean.class);
        factory.registerBeanDefinition("setterBean", bd3);
        
        SetterInjectionBean bean = factory.getBean("setterBean", SetterInjectionBean.class);
        
        assertNotNull(bean.getService(), "@Autowired setter should inject dependency");
    }

    @Test
    public void testCircularDependencyThreeWay() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            CircularServiceA3.class, CircularServiceB3.class, CircularServiceC3.class);
        CircularServiceA3 a = ctx.getBean(CircularServiceA3.class);
        CircularServiceB3 b3 = ctx.getBean(CircularServiceB3.class);
        CircularServiceC3 c = ctx.getBean(CircularServiceC3.class);
        assertNotNull(a);
        assertNotNull(b3);
        assertNotNull(c);
        assertSame(b3, a.getB());
        assertSame(c, b3.getC());
        assertSame(a, c.getA());
        ctx.close();
    }

    @Test
    public void testSelfReferencingBean() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            SelfReferencingBean.class);
        SelfReferencingBean bean = ctx.getBean(SelfReferencingBean.class);
        assertNotNull(bean);
        // 自引用时由于三级缓存机制会注入早期引用（指向自身）
        assertNotNull(bean.getSelf());
        assertSame(bean, bean.getSelf());
        ctx.close();
    }

    @Test
    public void testPreInstantiateOrder() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        factory.registerBeanDefinition("zBean", new BeanDefinition("zBean", IoCTest.SimpleBean.class));
        factory.registerBeanDefinition("aBean", new BeanDefinition("aBean", IoCTest.AnotherBean.class));
        factory.registerBeanDefinition("mBean", new BeanDefinition("mBean", IoCTest.SimpleBean.class));
        
        factory.preInstantiateSingletons();
        
        // 验证所有非懒加载的单例都被实例化了
        assertTrue(factory.containsBean("zBean"));
        assertTrue(factory.containsBean("aBean"));
        assertTrue(factory.containsBean("mBean"));
    }
}
