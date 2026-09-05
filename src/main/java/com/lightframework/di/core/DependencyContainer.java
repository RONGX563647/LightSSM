package com.lightframework.di.core;

import java.lang.reflect.Field;
import java.util.Map;

// TODO [L3][优化-工厂方法] getBean(name,type) / getBean(type) / resolveDependency / resolveDependencyWithGenerics / getBeansOfType 等多个解析入口语义相近，可统一为解析策略工厂：按传入参数（是否带名称、是否泛型、是否带 qualifier）选择对应 Resolver 实现，消除调用方与各入口内部的重复分支判断。；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
public interface DependencyContainer {
    <T> T getBean(String name, Class<T> requiredType) throws Exception;
    boolean containsBean(String name);
    Object createLazyProxy(Class<?> type, String beanName);
    // TODO [L2][练习] 手写一个最小 DependencyContainer 实现 - 用 Map<String,Object> 按名称、Map<Class<?>,Object> 按类型保存 bean；getBean(name) 按名取、getBean(type) 返回唯一同类型 bean（多个候选时按 @Primary 或抛错）、resolveDependency 按类型 + qualifier 查找；写对标志：getBean(Foo.class) 返回注册的同类型实例。
    Object resolveDependency(Class<?> type, String fieldName, String qualifier, boolean required) throws Exception;
    Object resolveDependencyWithGenerics(Field field, String qualifier, boolean required) throws Exception;
    <T> T getBean(Class<T> requiredType) throws Exception;
    <T> Map<String, T> getBeansOfType(Class<T> type) throws Exception;
}
