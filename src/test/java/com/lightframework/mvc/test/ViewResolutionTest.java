package com.lightframework.mvc.test;

import com.lightframework.mvc.view.InternalResourceView;
import com.lightframework.mvc.view.InternalResourceViewResolver;
import com.lightframework.mvc.view.RedirectView;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ViewResolutionTest {

    // 验证 TODO[L3][优化-策略模式] 练习目标：用户手写实现后运行本测试应全绿（按视图名选择不同 View）。
    @Test
    void resolvesInternalResourceViewByDefault() throws Exception {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        var view = resolver.resolveViewName("home");
        assertTrue(view instanceof InternalResourceView);
        assertEquals("/WEB-INF/views/home.jsp", ((InternalResourceView) view).getUrl());
    }

    @Test
    void resolvesRedirectView() throws Exception {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        var view = resolver.resolveViewName("redirect:/login");
        assertTrue(view instanceof RedirectView);
    }

    @Test
    void resolvesForwardViewAsInternalResource() throws Exception {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        var view = resolver.resolveViewName("forward:/a");
        assertTrue(view instanceof InternalResourceView);
    }

    @Test
    void nullViewNameReturnsNull() throws Exception {
        assertNull(new InternalResourceViewResolver().resolveViewName(null));
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（RedirectView 拼接 contextPath 并 sendRedirect）。
    @Test
    void redirectViewSendsRedirectWithContextPath() throws Exception {
        RedirectView view = new RedirectView("/target");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getContextPath()).thenReturn("/app");
        view.render(null, request, response);
        verify(response).sendRedirect("/app/target");
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（InternalResourceView 把 model 写入 request attribute 后 forward）。
    @Test
    void internalResourceViewSetsAttributesAndForwards() throws Exception {
        InternalResourceView view = new InternalResourceView("/p");
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getRequestDispatcher("/p")).thenReturn(mock(RequestDispatcher.class));
        view.render(Map.of("k", "v"), request, response);
        verify(request).setAttribute("k", "v");
        verify(request).getRequestDispatcher("/p");
    }
}
