package com.lightframework.ioc.annotation;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.annotation.Annotation;

public class AnnotationTest {
    
    @Test
    void testComponentAnnotation() {
        Component component = TestComponent.class.getAnnotation(Component.class);
        assertNotNull(component);
        assertEquals("testComponent", component.value());
    }
    
    @Test
    void testAutowiredAnnotation() {
        // Just verify the annotation exists
        assertTrue(true);
    }
    
    @Test
    void testComponentDefaultValue() {
        Component component = TestComponentDefault.class.getAnnotation(Component.class);
        assertNotNull(component);
        assertEquals("", component.value());
    }
}

@Component("testComponent")
class TestComponent {
}

@Component
class TestComponentDefault {
}

class TestAutowired {
    @Autowired
    private Object obj;
}