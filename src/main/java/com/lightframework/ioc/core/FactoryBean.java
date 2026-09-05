package com.lightframework.ioc.core;

/**
 * Interface to be implemented by beans which are factories for individual objects.
 * The actual object type returned by getObject() may differ from the FactoryBean's own type.
 * 
 * <p>FactoryBeans can support both singleton and prototype scopes,
 * and can properly create objects with lifecycle callbacks.
 * 
 * @param <T> the type of object that this FactoryBean creates
 */
public interface FactoryBean<T> {
    
    /**
     * Return an instance (possibly shared or independent) of the object managed by this factory.
     * 
     * @return an instance of the bean (can be {@code null})
     * @throws Exception if object creation failed
     */
    // TODO [L2][练习] 手写一个 FactoryBean（如简化版 SqlSessionFactoryBean：getObject() 创建目标对象，getObjectType() 返回其类型，isSingleton() 返回 true）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：把该 FactoryBean 注册为 Bean 后，getBean("name") 拿到 getObject() 的产物；getBean("&name") 拿到 FactoryBean 本身。
    T getObject() throws Exception;
    
    /**
     * Return the type of object that this FactoryBean creates.
     * 
     * @return the object type, or {@code null} if not known in advance
     */
    Class<?> getObjectType();
    
    /**
     * Is the object managed by this factory a singleton?
     * A singleton object is created once and cached for subsequent requests.
     * 
     * @return {@code true} if the object is a singleton (default), {@code false} otherwise
     */
    default boolean isSingleton() {
        return true;
    }
}
