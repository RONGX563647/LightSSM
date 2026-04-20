package com.lightframework.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Properties;

public class PropertyUtilTest {
    
    @Test
    void testLoadProperties() {
        Properties props = new Properties();
        props.setProperty("driver", "org.h2.Driver");
        props.setProperty("url", "jdbc:h2:mem:test");
        props.setProperty("username", "sa");
        
        assertNotNull(props);
        assertEquals("org.h2.Driver", props.getProperty("driver"));
        assertEquals("jdbc:h2:mem:test", props.getProperty("url"));
        assertEquals("sa", props.getProperty("username"));
    }
    
    @Test
    void testSetProperty() {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        
        assertEquals("value1", props.getProperty("key1"));
        
        props.setProperty("key1", "value2");
        assertEquals("value2", props.getProperty("key1"));
    }
    
    @Test
    void testContainsKey() {
        Properties props = new Properties();
        props.setProperty("test", "value");
        
        assertTrue(props.containsKey("test"));
        assertFalse(props.containsKey("nonexistent"));
    }
    
    @Test
    void testIsEmpty() {
        Properties props = new Properties();
        assertTrue(props.isEmpty());
        
        props.setProperty("key", "value");
        assertFalse(props.isEmpty());
    }
    
    @Test
    void testPropertyNames() {
        Properties props = new Properties();
        props.setProperty("key1", "value1");
        props.setProperty("key2", "value2");
        
        assertEquals(2, props.keySet().size());
    }
}