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
    
    // TODO [L1][练习] 理解 addInterceptor/addInterceptors 在写入后会置 compiled=false 触发下次查找重新 compile()。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（AdvisedSupportTest 中验证"先 getProxy 触发编译，
    // 再 addInterceptor，再 invoke"时新拦截器能生效，且 compiled 标志位行为正确）。
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
    // TODO [L2][优化-模板方法] findChainIndex 的"快速表 → 线性搜索 → equals 回退 → 接口-实现回退"四段查找流程；写对标志：按模板方法模式完成实现，新增单测覆盖“钩子方法被回调、算法骨架固定”的主路径与一条异常路径，断言执行顺序与结果正确。
    // 可用模板方法定义骨架，把每类回退作为可重写钩子，便于新增匹配策略(如桥接方法、泛型擦除后的签名归一)。
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
    
    // TODO [L2][练习] 实现 compile() 中 fastLookupTable 的开放寻址冲突处理——当前 hash 冲突时只把第一个空槽；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 放进去(getChainIndex 快速路径只在无冲突时命中)，冲突后退化成线性搜索。请线性探测下一个空槽，使 O(1) 查找真正成立。
    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（AdvisedSupportTest 中验证多个方法 hash 冲突时仍能正确定位链）。
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
    
    // 清理：原 getInterceptors/getMethodInterceptors 与 ArrayListView 为未使用的冗余访问器，已删除；
    // 外界统一通过 getChainIndex + getInterceptorChains/getTargetMethodHandles 索引访问。
    public boolean hasInterceptors() {
        return !this.methodInterceptors.isEmpty();
    }
    
    public boolean isCompiled() {
        return compiled;
    }
    
    // TODO [L3][优化-工厂方法] AdvisedSupport 同时承担"拦截器存储(methodInterceptors)"与"编译索引/MethodHandle 预编译"；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
    // 两类职责，可把高性能编译索引抽取为 ChainIndex 工厂产物(由 AdvisedSupport 工厂方法 createIndex() 生成)，
    // 职责更单一，也便于对"编译"做缓存/失效的单元测试。
    /** 暴露拦截器链数组供外部直接索引访问 */
    public MethodInterceptor[][] getInterceptorChains() {
        return interceptorChains;
    }
    
    /** 暴露 MethodHandle 数组供外部直接索引访问 */
    public MethodHandle[] getTargetMethodHandles() {
        return targetMethodHandles;
    }
}
