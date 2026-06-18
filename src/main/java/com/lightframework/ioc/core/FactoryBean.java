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
