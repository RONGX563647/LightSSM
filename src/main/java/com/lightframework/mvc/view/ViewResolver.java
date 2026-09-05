package com.lightframework.mvc.view;

public interface ViewResolver {
    // TODO [L3][优化-策略模式] 当前 ViewResolver 选择是 DispatcherServlet 里"遍历列表取第一个非空"；可把"按 viewName 前缀/后缀选择具体 Resolver"；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
    //   抽象为策略（InternalResourceStrategy / JsonStrategy / RedirectStrategy），由工厂按配置装配，消除硬编码的 redirect:/forward: 判断。
    View resolveViewName(String viewName) throws Exception;
}