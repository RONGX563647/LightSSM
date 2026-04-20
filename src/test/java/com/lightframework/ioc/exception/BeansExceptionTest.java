package com.lightframework.ioc.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BeansExceptionTest {
    
    @Test
    void testNoSuchBeanDefinitionExceptionWithName() {
        NoSuchBeanDefinitionException exception = new NoSuchBeanDefinitionException("myBean");
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("myBean"));
        assertNull(exception.getRequiredType());
    }
    
    @Test
    void testNoSuchBeanDefinitionExceptionWithType() {
        NoSuchBeanDefinitionException exception = new NoSuchBeanDefinitionException(String.class);
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("String"));
        assertEquals(String.class, exception.getRequiredType());
    }
    
    @Test
    void testNoSuchBeanDefinitionExceptionWithMessage() {
        NoSuchBeanDefinitionException exception = new NoSuchBeanDefinitionException(String.class, "Custom error message");
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Custom error message"));
        assertEquals(String.class, exception.getRequiredType());
    }
    
    @Test
    void testBeanCreationException() {
        BeanCreationException exception = new BeanCreationException("myBean", "Failed to create");
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("myBean"));
    }
    
    @Test
    void testBeanCreationExceptionWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        BeanCreationException exception = new BeanCreationException("myBean", "Failed to create", cause);
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
    }
}