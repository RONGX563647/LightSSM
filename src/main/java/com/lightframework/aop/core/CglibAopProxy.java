package com.lightframework.aop.core;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * CGLIB 代理实现
 * 
 * 修复：CGLIB Enhancer 回调竞争条件 + 多实例问题
 * 之前的问题1：共享 Enhancer → setCallback 被并发覆盖
 * 之前的问题2：缓存的 callback 绑定了旧的 AdvisedSupport → 新实例使用了错误回调
 * 现在：
 *   1. 每次创建新 Enhancer（无共享）
 *   2. 不缓存 callback（每个实例独立）
 *   3. 缓存 CGLIB 生成的代理类，避免重复生成字节码
 */
public class CglibAopProxy implements AopProxy {
    
    private final AdvisedSupport advised;
    
    public CglibAopProxy(AdvisedSupport advised) {
        this.advised = advised;
        this.advised.compile();
    }
    
    @Override
    public Object getProxy() {
        return getProxy(Thread.currentThread().getContextClassLoader());
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?> targetClass = advised.getTargetClass();
        
        try {
            net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
            enhancer.setClassLoader(classLoader);
            enhancer.setSuperclass(targetClass);
            enhancer.setCallback(new CglibMethodInterceptor(advised));
            // TODO [L2][练习] 实现 CGLIB 代理类缓存——当前 enhancer.create() 每次都重新生成字节码；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
            // (类注释声称要缓存却未实现)。请缓存生成的代理 Class(注意 Enhancer 不能共享且 callback 需绑定当前 advised)，
            // 使重复创建同类型代理不再生成字节码。验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿
            // （CglibAopProxyTest 验证多次 getProxy 产出不同实例但同类型、且行为一致）。
            return enhancer.create();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create CGLIB proxy", e);
        }
    }
    
    private static class CglibMethodInterceptor implements net.sf.cglib.proxy.MethodInterceptor {
        private final AdvisedSupport advised;
        
        CglibMethodInterceptor(AdvisedSupport advised) {
            this.advised = advised;
        }
        
        @Override
        public Object intercept(Object obj, Method method, Object[] args, 
            net.sf.cglib.proxy.MethodProxy proxy) throws Throwable {
            
            // 快速路径：Object 方法
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(obj, method, args);
            }
            
            Object target = advised.getTarget();
            
            // 单次查找
            int chainIdx = advised.getChainIndex(method);
            
            if (chainIdx < 0) {
                // 快速路径：使用 CGLIB MethodProxy（比反射快）
                return proxy.invoke(target, args);
            }
            
            MethodInterceptor[] chain = advised.getInterceptorChains()[chainIdx];
            MethodHandle targetMethodHandle = advised.getTargetMethodHandles()[chainIdx];
            
            MethodInvocation invocation = MethodInvocation.obtain(
                target, method, args, obj, chain, targetMethodHandle);
            
            try {
                return invocation.proceed();
            } finally {
                invocation.release();
            }
        }
        
        private Object handleObjectMethod(Object proxy, Method method, Object[] args) throws Throwable {
            // TODO [L1][练习] 理解 handleObjectMethod 对 CGLIB 代理 Object 方法的透传——当前 equals/hashCode/toString；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
            // 已处理；验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（CglibAopProxyTest 验证
            // 代理 toString 返回 "CGLIB Proxy: ..." 且未走拦截器链）。
            String name = method.getName();
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name)) {
                return "CGLIB Proxy: " + advised.getTargetClass().getName();
            }
            method.setAccessible(true);
            return method.invoke(proxy, args);
        }
    }
}
