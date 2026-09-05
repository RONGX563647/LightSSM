package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface HandlerInterceptor {
    // TODO [L1][练习] 默认三个方法都是空/true 实现；请手写一个拦截器（如日志、登录鉴权、耗时统计），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   覆写 preHandle 做前置校验、postHandle 修改 ModelAndView、afterCompletion 做资源清理，
    //   并把它注册为 Spring Bean（实现 HandlerInterceptor）使其被 RequestMappingHandlerMapping 收集为 globalInterceptors。
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
        Object handler) throws Exception {
        return true;
    }
    
    default void postHandle(HttpServletRequest request, HttpServletResponse response, 
        Object handler, ModelAndView modelAndView) throws Exception {
    }
    
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
        Object handler, Exception ex) throws Exception {
    }
}