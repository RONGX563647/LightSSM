package com.lightframework.mvc.test;

import com.lightframework.mvc.core.ContentNegotiationManager;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContentNegotiationManagerTest {

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（按 Accept 头协商 mediaType）。
    @Test
    void resolvesByAcceptHeader() {
        ContentNegotiationManager mgr = new ContentNegotiationManager();
        HttpServletRequest json = mock(HttpServletRequest.class);
        when(json.getHeader("Accept")).thenReturn("application/json");
        assertEquals("application/json", mgr.resolveMediaType(json));

        HttpServletRequest html = mock(HttpServletRequest.class);
        when(html.getHeader("Accept")).thenReturn("text/html");
        assertEquals("text/html", mgr.resolveMediaType(html));
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（无 Accept 时按 format 参数协商）。
    @Test
    void resolvesByFormatParamWhenNoAccept() {
        ContentNegotiationManager mgr = new ContentNegotiationManager();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn(null);
        when(req.getParameter("format")).thenReturn("xml");
        assertEquals("application/xml", mgr.resolveMediaType(req));
    }

    @Test
    void isJsonRequest() {
        ContentNegotiationManager mgr = new ContentNegotiationManager();
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getHeader("Accept")).thenReturn("application/json");
        assertTrue(mgr.isJsonRequest(req));
    }
}
