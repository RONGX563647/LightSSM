package com.lightframework.aop.core;

import java.util.List;

public class ProxyFactory {

    private AdvisedSupport advised;
    private boolean preferCglib = false;

    public ProxyFactory(Object target) {
        this.advised = new AdvisedSupport(target);
    }

    public void setPreferCglib(boolean preferCglib) {
        this.preferCglib = preferCglib;
    }

    public Object getProxy() {
        return createAopProxy().getProxy();
    }

    public Object getProxy(ClassLoader classLoader) {
        return createAopProxy().getProxy(classLoader);
    }

    protected AopProxy createAopProxy() {
        Class<?> targetClass = advised.getTargetClass();

        // TODO [L2][练习] 补充代理策略选择——当目标类 final 且无接口、且 preferCglib=false 时给出明确降级/报错；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        // 并补充 setPreferCglib 在已编译后重新选择代理类型的逻辑(当前 createAopProxy 每次新建，未复用缓存)。
        // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（ProxyFactoryTest 中验证 final 类/无接口/有接口三态选择）。

        // TODO [L3][优化-工厂方法] createAopProxy() 按条件返回 JdkDynamicAopProxy 或 CglibAopProxy 正是工厂方法模式；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
        // 可抽取 AopProxyFactory 接口 + JdkAopProxyFactory / CglibAopProxyFactory 实现类，把"JDK 还是 CGLIB"的决策
        // 从 ProxyFactory 移到工厂，消除下面的 if-else 分支，也便于新增第 3 种代理(如 ByteBuddy)。
        if (preferCglib || targetClass.getInterfaces().length == 0) {
            if (java.lang.reflect.Modifier.isFinal(targetClass.getModifiers())) {
                throw new IllegalArgumentException("Cannot create CGLIB proxy for final class: "
                    + targetClass.getName() + ". Use JDK proxy by not setting preferCglib.");
            }
            return new CglibAopProxy(advised);
        }

        return new JdkDynamicAopProxy(advised);
    }

    public void addInterceptor(java.lang.reflect.Method method, MethodInterceptor interceptor) {
        this.advised.addInterceptor(method, interceptor);
    }

    public void addInterceptors(java.lang.reflect.Method method, List<MethodInterceptor> interceptors) {
        this.advised.addInterceptors(method, interceptors);
    }

    public AdvisedSupport getAdvised() {
        return this.advised;
    }
}