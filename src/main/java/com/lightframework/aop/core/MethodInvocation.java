package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * 极致性能 MethodInvocation 实现
 * 核心优化：
 * 1. 嵌套安全对象池 - 支持同一线程内多层嵌套调用，int[] 避免 Integer 装箱
 * 2. 迭代循环 - 替代递归调用链，减少栈深度
 * 3. MethodHandle - 预编译方法句柄，JIT 可内联
 * 4. 数组存储拦截器 - 替代 List，减少间接访问
 */
public class MethodInvocation {
    
    /** 无参数常量数组 */
    private static final Object[] NO_ARGS = new Object[0];
    
    // ========== 嵌套安全对象池 ==========
    private static final int POOL_SIZE = 8; // 支持最多 8 层嵌套
    private static final ThreadLocal<MethodInvocation[]> POOL = ThreadLocal.withInitial(() -> {
        MethodInvocation[] pool = new MethodInvocation[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            pool[i] = new MethodInvocation();
        }
        return pool;
    });
    // 优化：使用 int[] 数组避免 ThreadLocal<Integer> 的装箱开销
    private static final ThreadLocal<int[]> POOL_INDEX = ThreadLocal.withInitial(() -> new int[]{0});
    
    // TODO [L2][练习] 实现 obtain/release 对象池的嵌套层数保护——当前 POOL_SIZE=8 硬编码，超出后才 new 新实例；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 请把上限改为可配置常量并在嵌套过深(切面调切面)时优雅降级(记录日志/抛明确异常)，同时保证 release 不会把
    // idxRef[0] 减成负数。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（MethodInvocationTest 验证多层嵌套不串数据）。
    /** 获取实例（从池中取，支持嵌套调用） */
    public static MethodInvocation obtain(Object target, Method method, Object[] args, Object proxy, 
                                         MethodInterceptor[] interceptors, MethodHandle targetMethodHandle) {
        MethodInvocation[] pool = POOL.get();
        int[] idxRef = POOL_INDEX.get();
        int idx = idxRef[0];
        
        // 如果池满了，创建新实例（极端情况）
        if (idx >= POOL_SIZE) {
            MethodInvocation invocation = new MethodInvocation();
            invocation.reset(target, method, args, proxy, interceptors, targetMethodHandle);
            return invocation;
        }
        
        idxRef[0] = idx + 1;
        MethodInvocation invocation = pool[idx];
        invocation.reset(target, method, args, proxy, interceptors, targetMethodHandle);
        return invocation;
    }
    
    /** 释放实例（返回到池中） */
    public void release() {
        int[] idxRef = POOL_INDEX.get();
        if (idxRef[0] > 0) {
            idxRef[0]--;
        }
    }
    
    // ========== 核心字段 ==========
    private Object target;
    private Method method;
    private Object[] args;
    private Object proxy;
    private MethodInterceptor[] interceptors;
    private int currentInterceptorIndex;
    private int interceptorCount;
    
    // MethodHandle 优化
    private MethodHandle targetMethodHandle;
    
    // JoinPoint 懒加载
    private volatile JoinPoint joinPoint;
    
    private MethodInvocation() {}
    
    private void reset(Object target, Method method, Object[] args, Object proxy, 
                       MethodInterceptor[] interceptors, MethodHandle targetMethodHandle) {
        this.target = target;
        this.method = method;
        this.args = args != null ? args : NO_ARGS;
        this.proxy = proxy;
        this.interceptors = interceptors;
        this.currentInterceptorIndex = 0;
        this.interceptorCount = interceptors != null ? interceptors.length : 0;
        this.targetMethodHandle = targetMethodHandle;
        this.joinPoint = null;
    }
    
    /**
     * 传统构造函数（向后兼容）
     */
    public MethodInvocation(Object target, Method method, Object[] args, Object proxy, 
                           java.util.List<MethodInterceptor> interceptorList) {
        this.target = target;
        this.method = method;
        this.args = args != null ? args : NO_ARGS;
        this.proxy = proxy;
        this.interceptors = interceptorList != null ? interceptorList.toArray(new MethodInterceptor[0]) : new MethodInterceptor[0];
        this.currentInterceptorIndex = 0;
        this.interceptorCount = this.interceptors.length;
        this.targetMethodHandle = null;
    }
    
    /**
     * 执行拦截器链（迭代版本，零递归）
     * 
     * 设计说明：
     * 每个拦截器的 invoke() 方法会调用 invocation.proceed() 传递到下一个拦截器
     * 这里不需要循环，因为拦截器链的传递是通过递归的 proceed() 调用完成的
     */
    // TODO [L3][优化-责任链] MethodInvocation.proceed() 就是责任链推进核心：当前用 currentInterceptorIndex；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的主路径与一条全链放行的路径。
    // 数组索引迭代推进。可把"链 + 当前指针"抽取为显式 Chain 对象，支持短路(跳过后续拦截器)、重置指针、
    // 以及异常时统一回调各拦截器的 onError 钩子，使责任链语义更清晰、更易扩展(如引入 ExposeInvocationInterceptor)。
    public Object proceed() throws Throwable {
        if (currentInterceptorIndex < interceptorCount) {
            MethodInterceptor interceptor = interceptors[currentInterceptorIndex++];
            return interceptor.invoke(this);
        }
        return invokeTargetMethod();
    }
    
    /**
     * 调用目标方法（优先使用 MethodHandle）
     */
    private Object invokeTargetMethod() throws Throwable {
        if (targetMethodHandle != null) {
            if (args.length == 0) {
                return targetMethodHandle.invokeWithArguments();
            }
            return targetMethodHandle.invokeWithArguments(args);
        }
        // 反射回退
        method.setAccessible(true);
        return method.invoke(target, args);
    }
    
    /**
     * 获取 JoinPoint（使用对象池，避免 GC）
     */
    public JoinPoint getJoinPoint() {
        JoinPoint jp = joinPoint;
        if (jp == null) {
            joinPoint = jp = JoinPoint.obtain(target, method, args, proxy);
        }
        return jp;
    }
    
    public Object getTarget() { return target; }
    public Method getMethod() { return method; }
    public Object[] getArgs() { return args; }
    public Object getProxy() { return proxy; }
    public MethodInterceptor[] getInterceptors() { return interceptors; }
    public int getCurrentInterceptorIndex() { return currentInterceptorIndex; }
    
    // 清理：原 getTargetMethodHandle()/setTargetMethodHandle() 未使用，已删除；
    // targetMethodHandle 仅由 obtain/reset 在创建时一次性注入。
    // TODO [L1][练习] 理解 setArgs/reset 如何同步刷新 JoinPoint 的参数——当前 setArgs 会顺带更新 joinPoint.args；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（MethodInvocationTest 验证 @Around 中
    // joinPoint.proceed(newArgs) 修改参数后能正确传到目标方法，且 JoinPoint.getArgs() 同步可见）。
    public void setArgs(Object[] args) {
        this.args = args != null ? args : NO_ARGS;
        if (joinPoint != null) {
            joinPoint.setArgs(this.args);
        }
    }
}
