package com.lightframework.mvc.test;

import com.lightframework.mvc.core.CorsProcessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorsProcessorTest {

    // 验证 TODO[L3] 练习目标：用户手写实现后运行本测试应全绿（预检请求正确设置 CORS 头并返回 false）。
    @Test
    void preflightSetsHeadersAndReturnsFalse() {
        CorsProcessor processor = new CorsProcessor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Origin")).thenReturn("http://a.com");
        when(request.getMethod()).thenReturn("OPTIONS");

        assertFalse(processor.processRequest(request, response));
        verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("http://a.com"));
        verify(response).setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @Test
    void noOriginHeaderReturnsTrue() {
        CorsProcessor processor = new CorsProcessor();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Origin")).thenReturn(null);
        assertTrue(processor.processRequest(request, response));
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（后缀通配 origin 匹配）。
    @Test
    void suffixWildcardOriginAllowed() {
        CorsProcessor processor = new CorsProcessor();
        processor.setAllowedOrigins(List.of("*.example.com"));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Origin")).thenReturn("http://api.example.com");
        when(request.getMethod()).thenReturn("GET");
        assertTrue(processor.processRequest(request, response));
        verify(response).setHeader(eq("Access-Control-Allow-Origin"), eq("http://api.example.com"));
    }
}
