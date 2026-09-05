package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;

public interface HandlerMapping {
    // TODO [L3][优化-工厂方法] 当前只有 RequestMappingHandlerMapping 一种实现；当引入静态资源映射、健康检查、WebSocket 等映射时，；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
    //   可用工厂方法 + 排序（Ordered）按配置创建并排序不同的 HandlerMapping 实现，DispatcherServlet 无需改动即可扩展。
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
    
    boolean supports(Object handler);
}