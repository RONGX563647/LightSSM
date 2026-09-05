package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

public class HandlerExecutionChain {
    // TODO [L3][优化-责任链] 当前拦截器链已用责任链模式，但拦截器的装配（globalInterceptors 简单 add）缺少排序/条件匹配能力。；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的主路径与一条全链放行的路径。
    //   可抽象出 InterceptorRegistration（含 order、includePaths/excludePaths），用建造者组装后由 HandlerExecutionChain 统一编排，
    //   使"哪些拦截器作用于哪些路径"可配置、可扩展。
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
        // TODO [L1][练习] 当前某个 preHandle 返回 false 时调用 triggerAfterCompletion(request, response, null)，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   依赖 interceptorIndex 逆序清理；请把"仅对已经成功的拦截器触发 afterCompletion"的短路逻辑抽取为独立可读方法，
        //   并补充单元测试验证：第 2 个拦截器拒绝时，只有第 1 个的 afterCompletion 被调用。
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