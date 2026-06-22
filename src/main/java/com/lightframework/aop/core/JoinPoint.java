package com.lightframework.aop.core;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 高性能 JoinPoint - 嵌套安全对象池
 * 
 * 修复：
 * 1. 使用数组对象池（支持最多 8 层嵌套调用）
 * 2. 预缓存字段避免重复方法调用
 * 
 * 之前的问题：ThreadLocal<JoinPoint> 每个线程只有 1 个实例
 * 嵌套调用时 reset() 会覆盖外层调用的数据
 */
public class JoinPoint {
    
    private static final Object[] NO_ARGS = new Object[0];
    private static final int POOL_SIZE = 8;
    
    // 修复：数组对象池，支持嵌套调用
    private static final ThreadLocal<JoinPoint[]> POOL = ThreadLocal.withInitial(() -> {
        JoinPoint[] pool = new JoinPoint[POOL_SIZE];
        for (int i = 0; i < POOL_SIZE; i++) {
            pool[i] = new JoinPoint();
        }
        return pool;
    });
    private static final ThreadLocal<int[]> POOL_INDEX = ThreadLocal.withInitial(() -> new int[]{0});
    
    public static JoinPoint obtain(Object target, Method method, Object[] args, Object proxy) {
        JoinPoint[] pool = POOL.get();
        int[] idxRef = POOL_INDEX.get();
        int idx = idxRef[0];
        
        if (idx >= POOL_SIZE) {
            // 极端情况：超过嵌套层数，创建临时实例
            JoinPoint jp = new JoinPoint();
            jp.reset(target, method, args, proxy);
            return jp;
        }
        
        idxRef[0] = idx + 1;
        JoinPoint jp = pool[idx];
        jp.reset(target, method, args, proxy);
        return jp;
    }
    
    private Object target;
    private Method method;
    private Object[] args;
    private Object proxy;
    private String methodName;
    private Class<?> targetClass;
    private String signature;
    
    private JoinPoint() {}
    
    public JoinPoint(Object target, Method method, Object[] args, Object proxy) {
        reset(target, method, args, proxy);
    }
    
    public void reset(Object target, Method method, Object[] args, Object proxy) {
        this.target = target;
        this.method = method;
        this.args = args != null ? args : NO_ARGS;
        this.proxy = proxy;
        this.methodName = method.getName();
        this.targetClass = target.getClass();
        this.signature = this.targetClass.getName() + '.' + this.methodName;
    }
    
    public Object getTarget() { return target; }
    public Method getMethod() { return method; }
    public Object[] getArgs() { return args; }
    public Object getProxy() { return proxy; }
    public String getMethodName() { return methodName; }
    public Class<?> getTargetClass() { return targetClass; }
    public String getSignature() { return signature; }
    public String getArgsString() { return Arrays.toString(args); }
    
    public void setArgs(Object[] args) {
        this.args = args != null ? args : NO_ARGS;
    }
}
