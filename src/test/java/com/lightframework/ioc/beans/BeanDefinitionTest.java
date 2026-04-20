package com.lightframework.ioc.beans;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeanDefinitionTest {
    
    @Test
    void testBeanDefinitionCreation() {
        BeanDefinition bd = new BeanDefinition();
        assertNotNull(bd);
    }
    
    @Test
    void testSetBeanClass() {
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(String.class);
        assertEquals(String.class, bd.getBeanClass());
    }
    
    @Test
    void testSetScope() {
        BeanDefinition bd = new BeanDefinition();
        bd.setScope("singleton");
        assertEquals("singleton", bd.getScope());
        
        bd.setScope("prototype");
        assertEquals("prototype", bd.getScope());
    }
    
    @Test
    void testIsSingleton() {
        BeanDefinition bd = new BeanDefinition();
        bd.setScope("singleton");
        assertTrue(bd.isSingleton());
        
        bd.setScope("prototype");
        assertFalse(bd.isSingleton());
    }
    
    @Test
    void testIsPrototype() {
        BeanDefinition bd = new BeanDefinition();
        bd.setScope("prototype");
        assertTrue(bd.isPrototype());
        
        bd.setScope("singleton");
        assertFalse(bd.isPrototype());
    }
    
    @Test
    void testSetBeanClassName() {
        BeanDefinition bd = new BeanDefinition();
        bd.setBeanClass(String.class);
        assertEquals(String.class, bd.getBeanClass());
    }
}