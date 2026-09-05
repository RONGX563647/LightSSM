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
    
    // TODO [L2][练习] 实现 JoinPoint.obtain 的嵌套对象池保护——与 MethodInvocation 同理，POOL_SIZE=8 超出时；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // new 临时实例；请保证多层嵌套调用(切面调切面)时内层 JoinPoint 数据不被外层 reset 覆盖，并写测试验证嵌套 AOP 的
    // args 正确。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（JoinPointTest 验证嵌套调用数据隔离）。
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
    // TODO [L1][练习] 理解 JoinPoint.getSignature()/getArgsString() 的构建——当前用 targetClass.methodName 拼签名；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（JoinPointTest 验证 getTarget() 返回真实 target、
    // getArgs() 返回调用参数、getSignature() 形如 "类.方法"）。
    public String getArgsString() { return Arrays.toString(args); }
    
    public void setArgs(Object[] args) {
        this.args = args != null ? args : NO_ARGS;
    }
}
