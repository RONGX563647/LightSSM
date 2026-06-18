package com.lightframework.ioc.scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 应用作用域实现
 * 整个应用程序生命周期内共享同一个 Bean 实例（类似 ServletContext 级别）
 * 
 * 性能优化：
 * 1. 使用 ConcurrentHashMap 实现线程安全缓存
 * 2. 双重检查锁定实现延迟初始化
 * 3. 使用 AtomicBoolean 防止重复注册回调
 * 4. 最小化同步开销，读操作无锁
 */
public class ApplicationScope implements Scope {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationScope.class);

    public static final String SCOPE_NAME = "application";

    /**
     * Bean 实例缓存（线程安全）
     */
    private final Map<String, Object> beanCache = new ConcurrentHashMap<>(64);

    /**
     * 销毁回调缓存
     */
    private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>(32);

    /**
     * 已注册回调的标记（防止重复注册）
     */
    private final Map<String, AtomicBoolean> registeredFlags = new ConcurrentHashMap<>(32);

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        // 快速路径：无锁读取
        Object bean = beanCache.get(name);
        if (bean != null) {
            return bean;
        }

        // 双重检查锁定创建
        synchronized (name.intern()) {
            bean = beanCache.get(name);
            if (bean == null) {
                try {
                    bean = objectFactory.getObject();
                    beanCache.put(name, bean);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created new application-scoped bean: '{}'", name);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create application-scoped bean '" + name + "'", e);
                }
            }
        }

        return bean;
    }

    @Override
    public Object remove(String name) {
        // 执行销毁回调
        Runnable callback = destructionCallbacks.remove(name);
        if (callback != null) {
            try {
                callback.run();
                if (logger.isDebugEnabled()) {
                    logger.debug("Executed destruction callback for bean '{}' in application scope", name);
                }
            } catch (Exception e) {
                logger.warn("Failed to execute destruction callback for bean '{}' in application scope: {}", name, e.getMessage());
            }
        }

        registeredFlags.remove(name);
        Object removed = beanCache.remove(name);

        if (logger.isDebugEnabled() && removed != null) {
            logger.debug("Removed application-scoped bean: '{}'", name);
        }

        return removed;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Destruction callback must not be null");
        }

        // 使用 AtomicBoolean 确保每个 Bean 只注册一次回调
        AtomicBoolean flag = registeredFlags.computeIfAbsent(name, k -> new AtomicBoolean(false));
        if (flag.compareAndSet(false, true)) {
            destructionCallbacks.put(name, callback);
            if (logger.isDebugEnabled()) {
                logger.debug("Registered destruction callback for bean '{}' in application scope", name);
            }
        }
    }

    @Override
    public String[] getBeanNames() {
        return beanCache.keySet().toArray(new String[0]);
    }

    @Override
    public String getConversationId() {
        return "application";
    }

    /**
     * 获取应用级缓存（用于与 SingletonCache 集成）
     */
    public Map<String, Object> getBeanCache() {
        return beanCache;
    }

    /**
     * 销毁所有应用级 Bean
     * 应在应用关闭时调用
     */
    public void destroyAll() {
        // 逆序销毁
        String[] beanNames = getBeanNames();
        for (int i = beanNames.length - 1; i >= 0; i--) {
            remove(beanNames[i]);
        }

        beanCache.clear();
        destructionCallbacks.clear();
        registeredFlags.clear();

        logger.info("All application-scoped beans destroyed");
    }
}
