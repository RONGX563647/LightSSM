package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Component
class CircularA {
    @Autowired
    private CircularB b;

    public CircularB getB() {
        return b;
    }
}

@Component
class CircularB {
    @Autowired
    private CircularA a;

    public CircularA getA() {
        return a;
    }
}

public class IoCTest {
    
    public static class SimpleBean {
        private String name = "SimpleBean";
        
        public String getName() {
            return name;
        }
    }
    
    public static class AnotherBean {
        private int value = 42;
        
        public int getValue() {
            return value;
        }
    }
    
    @Test
    public void testBeanRegistration() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd);
        
        BeanDefinition bd2 = new BeanDefinition();
        bd2.setBeanClass(AnotherBean.class);
        factory.registerBeanDefinition("anotherBean", bd2);
        
        assertTrue(factory.containsBeanDefinition("simpleBean"));
        assertTrue(factory.containsBeanDefinition("anotherBean"));
    }
    
    @Test
    public void testBeanCreation() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd);
        
        try {
            SimpleBean bean = factory.getBean("simpleBean", SimpleBean.class);
            assertNotNull(bean);
            assertEquals("SimpleBean", bean.getName());
        } catch (Exception e) {
            fail("Failed to create bean: " + e.getMessage());
        }
    }
    
    @Test
    public void testBeanScope() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        bd.setScope("singleton");
        factory.registerBeanDefinition("singletonBean", bd);
        
        BeanDefinition bd2 = new BeanDefinition();
        bd2.setBeanClass(AnotherBean.class);
        bd2.setScope("prototype");
        factory.registerBeanDefinition("prototypeBean", bd2);
        
        assertTrue(factory.isSingleton("singletonBean"));
        assertTrue(factory.isPrototype("prototypeBean"));
    }
    
    @Test
    public void testBeanType() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd);
        
        assertEquals(SimpleBean.class, factory.getType("simpleBean"));
    }
    
    @Test
    public void testBeanCount() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd1 = new BeanDefinition();
        bd1.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd1);
        
        BeanDefinition bd2 = new BeanDefinition();
        bd2.setBeanClass(AnotherBean.class);
        factory.registerBeanDefinition("anotherBean", bd2);
        
        assertEquals(2, factory.getBeanDefinitionCount());
    }
    
    @Test
    public void testSingletonReturnsSameInstance() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd);
        
        try {
            SimpleBean bean1 = factory.getBean("simpleBean", SimpleBean.class);
            SimpleBean bean2 = factory.getBean("simpleBean", SimpleBean.class);
            assertSame(bean1, bean2);
        } catch (Exception e) {
            fail("Failed to test singleton: " + e.getMessage());
        }
    }
    
    @Test
    public void testPrototypeReturnsDifferentInstances() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        bd.setScope("prototype");
        factory.registerBeanDefinition("prototypeBean", bd);
        
        try {
            SimpleBean bean1 = factory.getBean("prototypeBean", SimpleBean.class);
            SimpleBean bean2 = factory.getBean("prototypeBean", SimpleBean.class);
            assertNotSame(bean1, bean2);
        } catch (Exception e) {
            fail("Failed to test prototype: " + e.getMessage());
        }
    }
    
    @Test
    public void testContainsBean() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd);
        
        assertTrue(factory.containsBean("simpleBean"));
        assertFalse(factory.containsBean("nonExistentBean"));
    }
    
    @Test
    public void testGetBeanDefinitionNames() {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        
        BeanDefinition bd1 = new BeanDefinition();
        bd1.setBeanClass(SimpleBean.class);
        factory.registerBeanDefinition("simpleBean", bd1);
        
        BeanDefinition bd2 = new BeanDefinition();
        bd2.setBeanClass(AnotherBean.class);
        factory.registerBeanDefinition("anotherBean", bd2);
        
        String[] names = factory.getBeanDefinitionNames();
        assertEquals(2, names.length);
    }
    
    @Test
    public void testCircularDependency() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
            CircularA.class, CircularB.class);
        CircularA a = ctx.getBean(CircularA.class);
        assertNotNull(a);
        assertNotNull(a.getB());
        assertSame(a, a.getB().getA());
    }
}