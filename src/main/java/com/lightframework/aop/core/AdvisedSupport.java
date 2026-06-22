package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极致性能代理支持类
 * 核心优化：
 * 1. O(1) 拦截器查找 - identityHashCode 快速表 + 线性搜索
 * 2. 单次查找返回链+句柄 - 消除重复查找
 * 3. 预编译 MethodHandle - 代理创建时生成，运行时直接使用
 * 4. 缓存 target 引用 - 避免每次 getTarget() 读
 */
public class AdvisedSupport {
    
    /** O(1) 查找表大小 */
    private static final int LOOKUP_TABLE_SIZE = 256;
    
    private Object target;
    private Class<?> targetClass;
    
    // 原始存储（向后兼容）
    private final Map<Method, List<MethodInterceptor>> methodInterceptors = new ConcurrentHashMap<>();
    
    // 高性能存储
    private volatile boolean compiled = false;
    private volatile int[] methodIdentityHashes;
    private volatile Method[] compiledMethods;
    private volatile MethodInterceptor[][] interceptorChains;
    private volatile MethodHandle[] targetMethodHandles;
    private volatile int chainCount;
    private volatile int[] fastLookupTable;
    
    // 优化：缓存 target 引用，避免每次读取
    private Object cachedTarget;
    
    public AdvisedSupport(Object target) {
        this.target = target;
        this.cachedTarget = target;
        this.targetClass = target.getClass();
    }
    
    public Object getTarget() {
        return cachedTarget != null ? cachedTarget : target;
    }
    
    public void setTarget(Object target) {
        this.target = target;
        this.cachedTarget = target;
        this.targetClass = target.getClass();
        this.compiled = false;
    }
    
    public Class<?> getTargetClass() {
        return this.targetClass;
    }
    
    public void addInterceptor(Method method, MethodInterceptor interceptor) {
        List<MethodInterceptor> interceptors = this.methodInterceptors.computeIfAbsent(
            method, k -> new java.util.ArrayList<>());
        interceptors.add(interceptor);
        this.compiled = false;
    }
    
    public void addInterceptors(Method method, List<MethodInterceptor> interceptors) {
        List<MethodInterceptor> existingInterceptors = this.methodInterceptors.computeIfAbsent(
            method, k -> new java.util.ArrayList<>());
        existingInterceptors.addAll(interceptors);
        this.compiled = false;
    }
    
    /**
     * 单次查找同时返回拦截器链和目标方法句柄
     * 消除原来分开调用 getInterceptorChain() + getTargetMethodHandle() 的两次查找
     */
    public int findChainIndex(Method method) {
        if (!compiled) {
            compile();
        }
        if (chainCount == 0) return -1;
        
        // 快速路径：查表
        int methodHash = System.identityHashCode(method);
        int hashIndex = methodHash & (LOOKUP_TABLE_SIZE - 1);
        int candidateIndex = fastLookupTable[hashIndex];
        
        if (candidateIndex >= 0 && methodIdentityHashes[candidateIndex] == methodHash 
            && compiledMethods[candidateIndex] == method) {
            return candidateIndex;
        }
        
        // 线性搜索
        for (int i = 0; i < chainCount; i++) {
            if (methodIdentityHashes[i] == methodHash && compiledMethods[i] == method) {
                return i;
            }
        }
        
        // equals 回退
        for (int i = 0; i < chainCount; i++) {
            if (compiledMethods[i].equals(method)) {
                return i;
            }
        }
        
        // 接口-实现映射回退：JDK 代理传入接口方法，但 map 中存的是实现类方法
        String methodName = method.getName();
        Class<?>[] methodParamTypes = method.getParameterTypes();
        for (int i = 0; i < chainCount; i++) {
            Method storedMethod = compiledMethods[i];
            if (storedMethod.getName().equals(methodName) 
                && java.util.Arrays.equals(storedMethod.getParameterTypes(), methodParamTypes)) {
                return i;
            }
        }
        
        return -1;
    }
    
    public MethodInterceptor[] getInterceptorChain(Method method) {
        int idx = findChainIndex(method);
        return idx >= 0 ? interceptorChains[idx] : null;
    }
    
    public MethodHandle getTargetMethodHandle(Method method) {
        int idx = findChainIndex(method);
        return idx >= 0 ? targetMethodHandles[idx] : null;
    }
    
    /**
     * 获取拦截器链数组和 MethodHandle 索引（单次查找）
     * 返回索引，调用者直接用索引访问 interceptorChains[idx] 和 targetMethodHandles[idx]
     */
    public int getChainIndex(Method method) {
        return findChainIndex(method);
    }
    
    public synchronized void compile() {
        if (compiled) return;
        
        int size = methodInterceptors.size();
        if (size == 0) {
            compiled = true;
            methodIdentityHashes = new int[0];
            compiledMethods = new Method[0];
            interceptorChains = new MethodInterceptor[0][];
            targetMethodHandles = new MethodHandle[0];
            fastLookupTable = new int[LOOKUP_TABLE_SIZE];
            java.util.Arrays.fill(fastLookupTable, -1);
            chainCount = 0;
            return;
        }
        
        methodIdentityHashes = new int[size];
        compiledMethods = new Method[size];
        interceptorChains = new MethodInterceptor[size][];
        targetMethodHandles = new MethodHandle[size];
        fastLookupTable = new int[LOOKUP_TABLE_SIZE];
        java.util.Arrays.fill(fastLookupTable, -1);
        chainCount = size;
        
        int idx = 0;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Map.Entry<Method, List<MethodInterceptor>> entry : methodInterceptors.entrySet()) {
            Method m = entry.getKey();
            List<MethodInterceptor> interceptors = entry.getValue();
            
            methodIdentityHashes[idx] = System.identityHashCode(m);
            compiledMethods[idx] = m;
            interceptorChains[idx] = interceptors.toArray(new MethodInterceptor[0]);
            
            try {
                MethodHandle mh = lookup.unreflect(m);
                targetMethodHandles[idx] = mh.bindTo(target);
            } catch (IllegalAccessException e) {
                targetMethodHandles[idx] = null;
            }
            
            int hashIndex = methodIdentityHashes[idx] & (LOOKUP_TABLE_SIZE - 1);
            if (fastLookupTable[hashIndex] == -1) {
                fastLookupTable[hashIndex] = idx;
            }
            idx++;
        }
        compiled = true;
    }
    
    public List<MethodInterceptor> getInterceptors(Method method) {
        if (!compiled) compile();
        int index = findChainIndex(method);
        if (index >= 0) {
            return new ArrayListView<>(interceptorChains[index]);
        }
        return this.methodInterceptors.get(method);
    }
    
    public Map<Method, List<MethodInterceptor>> getMethodInterceptors() {
        return this.methodInterceptors;
    }
    
    public boolean hasInterceptors() {
        return !this.methodInterceptors.isEmpty();
    }
    
    public boolean isCompiled() {
        return compiled;
    }
    
    /** 暴露拦截器链数组供外部直接索引访问 */
    public MethodInterceptor[][] getInterceptorChains() {
        return interceptorChains;
    }
    
    /** 暴露 MethodHandle 数组供外部直接索引访问 */
    public MethodHandle[] getTargetMethodHandles() {
        return targetMethodHandles;
    }
    
    private static class ArrayListView<T> extends java.util.AbstractList<T> {
        private final T[] array;
        ArrayListView(T[] array) { this.array = array; }
        @Override public T get(int index) { return array[index]; }
        @Override public int size() { return array.length; }
    }
}
