package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 高性能 JDK 动态代理实现
 * 核心优化：
 * 1. 单次查找获取拦截器链索引 + MethodHandle 索引
 * 2. 缓存 target 引用
 * 3. 无拦截器快速路径
 * 4. 对象池 MethodInvocation
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    // TODO [L3][优化-模板方法] JdkDynamicAopProxy.invoke 与 CglibAopProxy.CglibMethodInterceptor.intercept 的主体逻辑；写对标志：按模板方法模式完成实现，新增单测覆盖“钩子方法被回调、算法骨架固定”的主路径与一条异常路径，断言执行顺序与结果正确。
    // (取链索引→取 chain/MethodHandle→obtain→proceed→release)高度重复，可抽取 AbstractAopProxy 基类 +
    // 模板方法 doInvoke(proxy, method, args)，仅把"代理对象来源/Object 方法处理"留给子类实现。
    private final AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
        this.advised.compile();
    }
    
    @Override
    public Object getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] proxiedInterfaces = advised.getTargetClass().getInterfaces();
        if (proxiedInterfaces.length == 0) {
            throw new IllegalArgumentException("Target class " + advised.getTargetClass()
                + " has no interfaces; JDK proxy cannot be used");
        }
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 快速路径：跳过 Object 方法
        if (method.getDeclaringClass() == Object.class) {
            // TODO [L2][练习] 完善 Object 方法(guarded)处理——当前已处理 equals/hashCode/toString，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
            // 请补充 getClass 返回代理类、clone 抛 CloneNotSupportedException 等边界。
            // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（JdkDynamicAopProxyTest 验证
            // proxy.getClass() 与 target 类的区别、equals/hashCode/toString 行为）。
            if ("equals".equals(method.getName()) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(method.getName())) {
                return "JDK Proxy: " + advised.getTargetClass().getName();
            }
        }
        
        Object target = advised.getTarget();
        
        // 单次查找，返回索引
        int chainIdx = advised.getChainIndex(method);
        
        if (chainIdx < 0) {
            // 快速路径：无拦截器，直接调用 MethodHandle
            MethodHandle mh = advised.getTargetMethodHandle(method);
            if (mh != null) {
                return mh.invokeWithArguments(args);
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        
        // 使用索引直接访问（零重复查找）
        MethodInterceptor[] chain = advised.getInterceptorChains()[chainIdx];
        MethodHandle targetMethodHandle = advised.getTargetMethodHandles()[chainIdx];
        
        MethodInvocation invocation = MethodInvocation.obtain(
            target, method, args, proxy, chain, targetMethodHandle);
        
        try {
            return invocation.proceed();
        } finally {
            invocation.release();
        }
    }
}
