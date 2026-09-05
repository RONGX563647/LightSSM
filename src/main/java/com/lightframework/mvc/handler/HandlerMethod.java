package com.lightframework.mvc.handler;

import com.lightframework.ioc.context.ApplicationContext;

import java.lang.reflect.Method;

public class HandlerMethod {
    private final String beanName;
    private final Method method;
    private final Class<?> beanType;
    private final ApplicationContext applicationContext;

    private final Object handler;

    public HandlerMethod(String beanName, Method method, ApplicationContext applicationContext)
        throws Exception {
        this.beanName = beanName;
        this.method = method;
        this.applicationContext = applicationContext;
        this.beanType = applicationContext.getType(beanName);
        this.handler = applicationContext.getBean(beanName);
    }

    public HandlerMethod(Object handler, Method method) {
        // TODO [L3][优化-工厂方法] 两个构造器语义不同（一个从容器按 beanName 取 Bean，一个直接持有 handler），容易被误用。；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
        //   可改为私有构造 + 静态工厂方法 createFromBean(beanName, method, ctx) 与 createFromHandler(handler, method)，
        //   让调用方无法选错创建路径，同时把"从上下文取 Bean"的副作用收敛到工厂内。
        this.beanName = null;
        this.method = method;
        this.applicationContext = null;
        this.beanType = handler.getClass();
        this.handler = handler;
    }

    public Object getBean() {
        return this.handler;
    }

    public Method getMethod() {
        return this.method;
    }

    public Class<?> getBeanType() {
        return this.beanType;
    }

    public String getBeanName() {
        return this.beanName;
    }

    public Class<?> getReturnType() {
        return this.method.getReturnType();
    }

    @Override
    public String toString() {
        return this.beanType.getSimpleName() + "#" + this.method.getName();
    }

    // TODO [L1][练习] HandlerMethod 目前没有 equals/hashCode；请基于 (beanType, method) 实现它们（注意 method 的桥接方法问题），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   使相同 Controller 的同一方法在缓存/Set 中能被正确去重。验收标准：两个指向同一方法的 HandlerMethod equals 为 true、hashCode 相等。
}
