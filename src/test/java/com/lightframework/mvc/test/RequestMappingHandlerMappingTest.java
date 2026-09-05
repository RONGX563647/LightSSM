package com.lightframework.mvc.test;

import com.lightframework.di.annotation.Controller;
import com.lightframework.ioc.context.ApplicationContext;
import com.lightframework.mvc.annotation.GetMapping;
import com.lightframework.mvc.annotation.PathVariable;
import com.lightframework.mvc.annotation.RequestMapping;
import com.lightframework.mvc.core.HandlerExecutionChain;
import com.lightframework.mvc.core.HandlerInterceptor;
import com.lightframework.mvc.handler.HandlerMethod;
import com.lightframework.mvc.handler.RequestMappingHandlerMapping;
import com.lightframework.mvc.handler.RequestMappingInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestMappingHandlerMappingTest {

    @Controller
    static class HelloController {
        @GetMapping("/hello")
        public String hello() {
            return "helloView";
        }

        @GetMapping("/user/{id}")
        public String user(@PathVariable("id") Long id) {
            return "u" + id;
        }
    }

    private RequestMappingHandlerMapping mapping;

    @BeforeEach
    void setUp() throws Exception {
        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeanDefinitionNames()).thenReturn(new String[]{"helloCtrl"});
        when(ctx.getType("helloCtrl")).thenReturn((Class) HelloController.class);
        when(ctx.getBean("helloCtrl")).thenReturn(new HelloController());
        when(ctx.getBeanNamesForType(HandlerInterceptor.class)).thenReturn(new String[0]);

        mapping = new RequestMappingHandlerMapping(ctx);
        mapping.initHandlerMethods();
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（匹配优先级 / 精确路由命中）。
    @Test
    void getHandlerResolvesExactRoute() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/hello");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");

        HandlerExecutionChain chain = mapping.getHandler(request);
        assertNotNull(chain);
        assertTrue(chain.getHandler() instanceof HandlerMethod);
        HandlerMethod hm = (HandlerMethod) chain.getHandler();
        assertEquals("hello", hm.getMethod().getName());
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（路径变量匹配并把变量写入 request 属性）。
    @Test
    void getHandlerResolvesPathVariableAndBindsAttribute() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/user/42");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");

        HandlerExecutionChain chain = mapping.getHandler(request);
        assertNotNull(chain);

        org.mockito.ArgumentCaptor<Object> valueCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(request).setAttribute(eq(RequestMappingInfo.PATH_VARIABLES_ATTRIBUTE), valueCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, String> vars = (Map<String, String>) valueCaptor.getValue();
        assertEquals("42", vars.get("id"));
    }

    @Test
    void getHandlerReturnsNullWhenNoMatch() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/missing");
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        assertNull(mapping.getHandler(request));
    }
}
