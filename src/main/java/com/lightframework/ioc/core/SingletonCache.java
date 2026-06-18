package com.lightframework.ioc.core;

import com.lightframework.ioc.exception.BeanCreationException;
import com.lightframework.ioc.exception.BeanCurrentlyInCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 三级缓存管理：负责单例 Bean 的缓存、早期引用暴露、循环依赖解决
 * 设计模式：组合模式 - 封装三级缓存的复杂性
 * 线程安全：getSingleton 使用 synchronized(this) 保证三级缓存操作的原子性（与 Spring 一致）
 */
public class SingletonCache {

    private static final Logger logger = LoggerFactory.getLogger(SingletonCache.class);

    // 一级缓存：完全初始化的单例 Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);

    // 二级缓存：早期暴露的单例 Bean（未完全初始化）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

    // 三级缓存：单例工厂（用于创建早期引用）
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

    // 记录正在创建中的 Bean
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));

    // 已创建的 Bean 名称集合
    private final Set<String> createdBeanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(256));

    // Phase 2: creation stack for circular dependency chain tracking
    private final ThreadLocal<Deque<String>> creationStack =
            ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * 获取单例 Bean（三级缓存查找逻辑）
     * 线程安全：使用 synchronized(this) 保证三级缓存操作的原子性，与 Spring Framework 一致
     */
    public Object getSingleton(String beanName) {
        // 1. 一级缓存（无锁快速路径）
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            // 2/3 级缓存需要加锁，防止并发创建同一 Bean 的多个实例
            synchronized (this.singletonObjects) {
                // 双重检查：可能在等待锁期间已被其他线程初始化
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    // 2. 二级缓存
                    singletonObject = this.earlySingletonObjects.get(beanName);
                    if (singletonObject == null) {
                        // 3. 三级缓存获取工厂
                        ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                        if (singletonFactory != null) {
                            try {
                                singletonObject = singletonFactory.getObject();
                            } catch (Exception e) {
                                throw new BeanCreationException(beanName, "Failed to create singleton bean from early reference", e);
                            }
                            this.earlySingletonObjects.put(beanName, singletonObject);
                            this.singletonFactories.remove(beanName);
                            if (logger.isDebugEnabled()) logger.debug("Exposed early singleton bean: {}", beanName);
                        }
                    }
                }
            }
        }
        return singletonObject;
    }

    /**
     * 添加完整初始化的单例到一级缓存
     */
    public void addSingleton(String beanName, Object singletonObject) {
        synchronized (this.singletonObjects) {
            this.singletonObjects.put(beanName, singletonObject);
            this.singletonFactories.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
        }
        this.createdBeanNames.add(beanName);
        if (logger.isDebugEnabled()) logger.debug("Added singleton bean to cache: {}", beanName);
    }

    /**
     * 添加单例工厂到三级缓存（用于解决循环依赖）
     */
    public void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
        this.singletonFactories.put(beanName, singletonFactory);
        this.earlySingletonObjects.remove(beanName);
        if (logger.isDebugEnabled()) logger.debug("Added singleton factory for bean: {}", beanName);
    }

    /**
     * 标记 Bean 开始创建（带循环依赖检测）
     */
    public void beforeSingletonCreation(String beanName) {
        Deque<String> stack = creationStack.get();
        if (!this.singletonsCurrentlyInCreation.add(beanName)) {
            // 检测到循环依赖，构建完整调用链
            StringBuilder sb = new StringBuilder(256);
            sb.append("Circular dependency detected:\n");
            boolean started = false;
            for (String name : stack) {
                if (name.equals(beanName)) {
                    if (started) { sb.append("  -> ").append(name).append("  ← back to start\n"); break; }
                    started = true;
                }
                if (started) sb.append("  -> ").append(name).append("\n");
            }
            throw new BeanCurrentlyInCreationException(beanName, sb.toString());
        }
        stack.push(beanName);
    }

    /**
     * 标记 Bean 创建完成
     */
    public void afterSingletonCreation(String beanName) {
        this.singletonsCurrentlyInCreation.remove(beanName);
        Deque<String> stack = creationStack.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * 检查 Bean 是否正在创建中
     */
    public boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }

    /**
     * 检查 Bean 是否已创建
     */
    public boolean isBeanCreated(String beanName) {
        return this.createdBeanNames.contains(beanName);
    }

    /**
     * 获取创建中的 Bean 调用链
     */
    public Deque<String> getCreationStack() {
        return creationStack.get();
    }

    /**
     * 销毁所有单例 Bean 并清空缓存
     */
    public void destroySingletons(BiConsumer<String, Object> destroyCallback) {
        List<String> beanNames;
        synchronized (this.singletonObjects) {
            beanNames = new ArrayList<>(this.singletonObjects.keySet());
        }
        // 逆序销毁（依赖的 Bean 先销毁）
        for (int i = beanNames.size() - 1; i >= 0; i--) {
            String beanName = beanNames.get(i);
            Object bean = this.singletonObjects.get(beanName);
            if (bean != null && destroyCallback != null) {
                destroyCallback.accept(beanName, bean);
            }
        }
        clearAllCaches();
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        synchronized (this.singletonObjects) {
            this.singletonObjects.clear();
            this.earlySingletonObjects.clear();
            this.singletonFactories.clear();
        }
        this.singletonsCurrentlyInCreation.clear();
        this.createdBeanNames.clear();
        creationStack.remove();
    }

    /**
     * 移除指定 Bean 的单例缓存
     */
    public void removeSingleton(String beanName) {
        synchronized (this.singletonObjects) {
            this.singletonObjects.remove(beanName);
            this.earlySingletonObjects.remove(beanName);
            this.singletonFactories.remove(beanName);
        }
        this.createdBeanNames.remove(beanName);
        this.singletonsCurrentlyInCreation.remove(beanName);
    }

    /**
     * 获取单例 Bean 集合（只读）
     */
    public Map<String, Object> getSingletonObjects() {
        return Collections.unmodifiableMap(singletonObjects);
    }

    /**
     * 获取单例数量
     */
    public int getSingletonCount() {
        return this.singletonObjects.size();
    }

    /**
     * ObjectFactory 接口：延迟创建单例 Bean 的工厂
     */
    @FunctionalInterface
    public interface ObjectFactory<T> {
        T getObject() throws Exception;
    }
}
