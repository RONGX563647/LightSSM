package com.lightframework.mvc.test;

import com.lightframework.mvc.core.HandlerExecutionChain;
import com.lightframework.mvc.core.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HandlerExecutionChainTest {

    private final Object handler = new Object();

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（preHandle 短路时只触发已通过拦截器的 afterCompletion）。
    @Test
    void applyPreHandleReturnsTrueWhenAllPass() throws Exception {
        HandlerInterceptor i1 = mock(HandlerInterceptor.class);
        HandlerInterceptor i2 = mock(HandlerInterceptor.class);
        when(i1.preHandle(any(), any(), any())).thenReturn(true);
        when(i2.preHandle(any(), any(), any())).thenReturn(true);

        HandlerExecutionChain chain = new HandlerExecutionChain(handler);
        chain.addInterceptor(i1);
        chain.addInterceptor(i2);

        assertTrue(chain.applyPreHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class)));
        verify(i1).preHandle(any(), any(), any());
        verify(i2).preHandle(any(), any(), any());
    }

    @Test
    void applyPreHandleShortCircuitsAndTriggersAfterCompletionOnlyForPassed() throws Exception {
        HandlerInterceptor i1 = mock(HandlerInterceptor.class);
        HandlerInterceptor i2 = mock(HandlerInterceptor.class);
        when(i1.preHandle(any(), any(), any())).thenReturn(true);
        when(i2.preHandle(any(), any(), any())).thenReturn(false);

        HandlerExecutionChain chain = new HandlerExecutionChain(handler);
        chain.addInterceptor(i1);
        chain.addInterceptor(i2);

        assertFalse(chain.applyPreHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class)));
        verify(i1).afterCompletion(any(), any(), any(), any());
        verify(i2, never()).afterCompletion(any(), any(), any(), any());
    }

    @Test
    void applyPostHandleRunsInReverseOrder() throws Exception {
        HandlerInterceptor i1 = mock(HandlerInterceptor.class);
        HandlerInterceptor i2 = mock(HandlerInterceptor.class);
        HandlerExecutionChain chain = new HandlerExecutionChain(handler);
        chain.addInterceptor(i1);
        chain.addInterceptor(i2);

        chain.applyPostHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class), null);
        org.mockito.InOrder order = inOrder(i2, i1);
        order.verify(i2).postHandle(any(), any(), any(), any());
        order.verify(i1).postHandle(any(), any(), any(), any());
    }

    @Test
    void triggerAfterCompletionRunsInReverseOrder() throws Exception {
        HandlerInterceptor i1 = mock(HandlerInterceptor.class);
        HandlerInterceptor i2 = mock(HandlerInterceptor.class);
        when(i1.preHandle(any(), any(), any())).thenReturn(true);
        when(i2.preHandle(any(), any(), any())).thenReturn(true);
        HandlerExecutionChain chain = new HandlerExecutionChain(handler);
        chain.addInterceptor(i1);
        chain.addInterceptor(i2);

        // triggerAfterCompletion 依赖 applyPreHandle 设置的 interceptorIndex
        chain.applyPreHandle(mock(HttpServletRequest.class), mock(HttpServletResponse.class));
        chain.triggerAfterCompletion(mock(HttpServletRequest.class), mock(HttpServletResponse.class), null);
        org.mockito.InOrder order = inOrder(i2, i1);
        order.verify(i2).afterCompletion(any(), any(), any(), any());
        order.verify(i1).afterCompletion(any(), any(), any(), any());
    }
}
