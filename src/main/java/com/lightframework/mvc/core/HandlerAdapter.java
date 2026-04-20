package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerAdapter {
    boolean supports(Object handler);
    
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, 
        Object handler) throws Exception;
    
    long getLastModified(HttpServletRequest request, Object handler);
}