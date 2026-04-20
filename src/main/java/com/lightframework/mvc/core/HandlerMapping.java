package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;

public interface HandlerMapping {
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
    
    boolean supports(Object handler);
}