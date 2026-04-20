package com.lightframework.mvc.test;

import com.lightframework.mvc.annotation.RequestMapping;
import com.lightframework.mvc.annotation.ResponseBody;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class MvcTest {
    
    private TestController controller;
    
    @BeforeEach
    void setUp() {
        controller = new TestController();
    }
    
    @Test
    void testControllerMethods() {
        String result = controller.hello();
        assertEquals("Hello, World!", result);
    }
    
    @Test
    void testGetUser() {
        String result = controller.getUser();
        assertEquals("user1", result);
    }
}

class TestController {
    
    @RequestMapping("/hello")
    @ResponseBody
    public String hello() {
        return "Hello, World!";
    }
    
    @RequestMapping("/user")
    @ResponseBody
    public String getUser() {
        return "user1";
    }
    
    @RequestMapping("/echo")
    @ResponseBody
    public String echo(String name) {
        return "Echo: " + name;
    }
}
