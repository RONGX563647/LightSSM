package com.lightframework.aop.core;

import java.lang.reflect.Method;

/**
 * ProceedingJoinPoint - 用于 @Around 通知
 * 
 * 修复：持有 MethodInvocation 引用，proceed() 直接调用 invocation.proceed()
 * 这样可以正确传递到拦截器链的下一个拦截器，最终到达目标方法
 * 
 * 不再使用对象池（避免嵌套调用数据损坏），每次创建轻量实例
 */
public class ProceedingJoinPoint {
    
    private final MethodInvocation invocation;
    
    /**
     * 创建 ProceedingJoinPoint，绑定到当前 MethodInvocation
     */
    public ProceedingJoinPoint(MethodInvocation invocation) {
        this.invocation = invocation;
    }
    
    /**
     * 执行拦截器链的下一个拦截器（或目标方法）
     * 等价于 invocation.proceed()
     */
    public Object proceed() throws Throwable {
        return invocation.proceed();
    }
    
    /**
     * 使用新参数执行拦截器链的下一个拦截器（或目标方法）
     * 可以修改传递给目标方法的参数
     */
    public Object proceed(Object[] args) throws Throwable {
        invocation.setArgs(args);
        return invocation.proceed();
    }
    
    // ========== JoinPoint 委托方法 ==========
    
    public Object getTarget() { return invocation.getTarget(); }
    public Method getMethod() { return invocation.getMethod(); }
    public Object[] getArgs() { return invocation.getArgs(); }
    public Object getProxy() { return invocation.getProxy(); }
    
    public String getMethodName() { return invocation.getMethod().getName(); }
    public Class<?> getTargetClass() { return invocation.getTarget().getClass(); }
    
    public String getSignature() {
        return getTargetClass().getName() + '.' + getMethodName();
    }
    
    public String getArgsString() {
        return java.util.Arrays.toString(getArgs());
    }
}
