package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * 高性能复合拦截器 - 扁平化调用链
 * 
 * 将 Before、After、Around 通知合并为单个拦截器，减少方法调用层数。
 * 传统方式：MethodInvocation.proceed() -> Before.invoke() -> proceed() -> After.invoke() -> proceed() -> Target
 * 优化方式：CompositeInterceptor.invoke() -> Before handles -> Around/Target -> After handles
 * 
 * 性能优化：
 * 1. 减少方法调用层数（从 N+2 降至 1-2）
 * 2. 使用 MethodHandle 直接调用通知方法
 * 3. 避免递归调用栈增长
 * 4. JIT 更容易内联整个调用链
 */
public class CompositeInterceptor implements MethodInterceptor {
    
    private static final Object[] NO_ARGS = new Object[0];
    
    // Before 通知方法句柄数组
    private final MethodHandle[] beforeHandles;
    
    // After 通知方法句柄数组  
    private final MethodHandle[] afterHandles;
    
    // Around 通知方法句柄（最多一个）
    private final MethodHandle aroundHandle;
    
    // 目标方法句柄（由 MethodInvocation 提供）
    
    public CompositeInterceptor(MethodHandle[] beforeHandles, MethodHandle[] afterHandles, 
                               MethodHandle aroundHandle) {
        this.beforeHandles = beforeHandles != null ? beforeHandles : new MethodHandle[0];
        this.afterHandles = afterHandles != null ? afterHandles : new MethodHandle[0];
        this.aroundHandle = aroundHandle;
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        
        // 1. 执行 Before 通知
        for (MethodHandle handle : beforeHandles) {
            handle.invokeWithArguments(args.length > 0 ? new Object[]{invocation.getJoinPoint()} : NO_ARGS);
        }
        
        Object result;
        
        // 2. 执行 Around 通知或目标方法
        if (aroundHandle != null) {
            result = aroundHandle.invokeWithArguments(invocation.getJoinPoint());
        } else {
            // 继续执行拦截器链（如果有更多拦截器）或调用目标方法
            result = invocation.proceed();
        }
        
        // 3. 执行 After 通知
        for (MethodHandle handle : afterHandles) {
            handle.invokeWithArguments(args.length > 0 ? new Object[]{invocation.getJoinPoint()} : NO_ARGS);
        }
        
        return result;
    }
    
    /**
     * 构建器模式创建 CompositeInterceptor
     */
    public static class Builder {
        private java.util.List<MethodHandle> beforeHandles = new java.util.ArrayList<>();
        private java.util.List<MethodHandle> afterHandles = new java.util.ArrayList<>();
        private MethodHandle aroundHandle;
        
        public Builder addBefore(MethodHandle handle) {
            beforeHandles.add(handle);
            return this;
        }
        
        public Builder addAfter(MethodHandle handle) {
            afterHandles.add(handle);
            return this;
        }
        
        public Builder setAround(MethodHandle handle) {
            this.aroundHandle = handle;
            return this;
        }
        
        public CompositeInterceptor build() {
            return new CompositeInterceptor(
                beforeHandles.toArray(new MethodHandle[0]),
                afterHandles.toArray(new MethodHandle[0]),
                aroundHandle
            );
        }
        
        public boolean isEmpty() {
            return beforeHandles.isEmpty() && afterHandles.isEmpty() && aroundHandle == null;
        }
    }
}
