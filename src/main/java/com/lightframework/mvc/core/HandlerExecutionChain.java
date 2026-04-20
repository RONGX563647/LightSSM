package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public class HandlerExecutionChain {
    private final Object handler;
    private final List<HandlerInterceptor> interceptors = new ArrayList<>();
    private int interceptorIndex = -1;
    
    public HandlerExecutionChain(Object handler) {
        this.handler = handler;
    }
    
    public void addInterceptor(HandlerInterceptor interceptor) {
        this.interceptors.add(interceptor);
    }
    
    public Object getHandler() {
        return this.handler;
    }
    
    public List<HandlerInterceptor> getInterceptors() {
        return this.interceptors;
    }
    
    public boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        for (int i = 0; i < this.interceptors.size(); i++) {
            HandlerInterceptor interceptor = this.interceptors.get(i);
            if (!interceptor.preHandle(request, response, this.handler)) {
                triggerAfterCompletion(request, response, null);
                return false;
            }
            this.interceptorIndex = i;
        }
        return true;
    }
    
    public void applyPostHandle(HttpServletRequest request, HttpServletResponse response, 
        ModelAndView modelAndView) throws Exception {
        for (int i = this.interceptors.size() - 1; i >= 0; i--) {
            this.interceptors.get(i).postHandle(request, response, this.handler, modelAndView);
        }
    }
    
    public void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, 
        Exception ex) throws Exception {
        for (int i = this.interceptorIndex; i >= 0; i--) {
            this.interceptors.get(i).afterCompletion(request, response, this.handler, ex);
        }
    }
}