package com.lightframework.ioc.scope;

/**
 * Bean 作用域核心接口
 * 定义了不同生命周期作用域的统一抽象，支持 singleton、prototype、request、session、application 等
 * 
 * 设计模式：策略模式 - 不同 Scope 实现提供不同的 Bean 获取/销毁策略
 */
public interface Scope {

    /**
     * 从当前作用域获取 Bean 实例
     * 如果不存在，则使用 ObjectFactory 创建新实例并存入作用域
     *
     * @param name Bean 名称
     * @param objectFactory Bean 创建工厂（延迟创建）
     * @return Bean 实例
     */
    // TODO [L2][练习] 手写一个自定义 ThreadScope 实现 Scope 接口（每个线程持有独立实例，get 时从 ThreadLocal 取/创建，remove 时清理）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：同一线程多次 get 拿到同一实例；不同线程拿到不同实例。
    Object get(String name, ObjectFactory<?> objectFactory);

    /**
     * 从当前作用域中移除指定 Bean
     * 用于手动清理或作用域销毁时清理
     *
     * @param name Bean 名称
     * @return 被移除的 Bean 实例，如果不存在则返回 null
     */
    Object remove(String name);

    /**
     * 注册作用域销毁回调
     * 当作用域结束时（如请求结束、Session 过期），调用这些回调执行清理工作
     *
     * @param name 回调名称（用于标识）
     * @param callback 销毁回调
     */
    void registerDestructionCallback(String name, Runnable callback);

    /**
     * 获取作用域内所有 Bean 名称
     *
     * @return Bean 名称集合（只读）
     */
    String[] getBeanNames();

    /**
     * 获取作用域对话标识符
     * 例如 request 作用域返回请求 ID，session 作用域返回 Session ID
     *
     * @return 作用域标识，若不适用则返回 null
     */
    String getConversationId();

    /**
     * Bean 创建工厂接口（延迟初始化）
     * 用于在需要时才创建 Bean 实例，避免不必要的对象创建
     */
    @FunctionalInterface
    interface ObjectFactory<T> {
        T getObject() throws Exception;
    }
}
