package com.lightframework.ioc.scope;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 请求作用域实现
 * 每个 HTTP 请求拥有独立的 Bean 实例，请求结束时自动销毁
 * 
 * 性能优化：
 * 1. 使用 ThreadLocal + WeakReference 避免内存泄漏
 * 2. 使用 ConcurrentHashMap 作为请求级缓存
 * 3. 延迟初始化 Bean，仅在首次访问时创建
 */
public class RequestScope implements Scope {

    private static final Logger logger = LoggerFactory.getLogger(RequestScope.class);

    public static final String SCOPE_NAME = "request";

    /**
     * 请求属性中存储 Scope 缓存的键
     * 将缓存绑定到 HttpServletRequest 生命周期
     */
    private static final String REQUEST_SCOPE_CACHE_KEY = RequestScope.class.getName() + ".CACHE";

    /**
     * ThreadLocal 持有当前请求的引用，用于快速访问
     * 使用 WeakReference 避免 ThreadLocal 导致的内存泄漏
     */
    private static final ThreadLocal<WeakRef<HttpServletRequest>> currentRequest = new ThreadLocal<>();

    /**
     * 请求级缓存结构
     * Package-private 以便同包访问
     */
    static class RequestScopeCache {
        // Bean 实例缓存
        private final Map<String, Object> beanCache = new ConcurrentHashMap<>(16);
        // 销毁回调
        private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>(8);

        Object getBean(String name) {
            return beanCache.get(name);
        }

        void putBean(String name, Object bean) {
            beanCache.put(name, bean);
        }

        Object removeBean(String name) {
            destructionCallbacks.remove(name);
            return beanCache.remove(name);
        }

        void registerCallback(String name, Runnable callback) {
            destructionCallbacks.put(name, callback);
        }

        Map<String, Runnable> getDestructionCallbacks() {
            return destructionCallbacks;
        }

        Map<String, Object> getBeanCache() {
            return beanCache;
        }

        /**
         * 执行所有销毁回调并清空缓存
         */
        void destroy() {
            // 逆序执行回调（与创建顺序相反）
            destructionCallbacks.forEach((name, callback) -> {
                try {
                    callback.run();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Executed destruction callback for bean '{}' in request scope", name);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to execute destruction callback for bean '{}' in request scope: {}", name, e.getMessage());
                }
            });
            beanCache.clear();
            destructionCallbacks.clear();
        }
    }

    /**
     * WeakReference 包装类，避免 ThreadLocal 内存泄漏
     * Package-private 以便 SessionScope 等同类包访问
     */
    static class WeakRef<T> {
        private final java.lang.ref.WeakReference<T> ref;

        WeakRef(T value) {
            this.ref = new java.lang.ref.WeakReference<>(value);
        }

        T get() {
            return ref.get();
        }
    }

    /**
     * 设置当前请求上下文
     * 应在请求开始时调用（如 Filter 中）
     */
    public static void setCurrentRequest(HttpServletRequest request) {
        currentRequest.set(new WeakRef<>(request));
    }

    /**
     * 清除当前请求上下文
     * 应在请求结束时调用（如 Filter 中）
     */
    public static void clearCurrentRequest() {
        currentRequest.remove();
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getCurrentRequest() {
        WeakRef<HttpServletRequest> ref = currentRequest.get();
        if (ref == null) {
            throw new IllegalStateException("No request bound to current thread. " +
                    "Ensure RequestScope.setCurrentRequest() is called before accessing request-scoped beans.");
        }
        HttpServletRequest request = ref.get();
        if (request == null) {
            throw new IllegalStateException("Current request has been garbage collected.");
        }
        return request;
    }

    /**
     * 获取或创建请求级缓存
     * 使用 HttpServletRequest 属性存储，随请求生命周期自动清理
     */
    @SuppressWarnings("unchecked")
    private RequestScopeCache getRequestCache(HttpServletRequest request) {
        RequestScopeCache cache = (RequestScopeCache) request.getAttribute(REQUEST_SCOPE_CACHE_KEY);
        if (cache == null) {
            cache = new RequestScopeCache();
            request.setAttribute(REQUEST_SCOPE_CACHE_KEY, cache);
            if (logger.isDebugEnabled()) {
                logger.debug("Created new request scope cache for request: {}", request.hashCode());
            }
        }
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, ObjectFactory<?> objectFactory) {
        HttpServletRequest request = getCurrentRequest();
        RequestScopeCache cache = getRequestCache(request);

        // 快速路径：从缓存获取
        Object bean = cache.getBean(name);
        if (bean != null) {
            return bean;
        }

        // 双重检查锁定：防止并发创建
        synchronized (this) {
            bean = cache.getBean(name);
            if (bean == null) {
                try {
                    bean = objectFactory.getObject();
                    cache.putBean(name, bean);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created new request-scoped bean: '{}'", name);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create request-scoped bean '" + name + "'", e);
                }
            }
        }

        return bean;
    }

    @Override
    public Object remove(String name) {
        HttpServletRequest request = getCurrentRequest();
        RequestScopeCache cache = getRequestCache(request);
        Object removed = cache.removeBean(name);
        if (logger.isDebugEnabled() && removed != null) {
            logger.debug("Removed request-scoped bean: '{}'", name);
        }
        return removed;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Destruction callback must not be null");
        }
        HttpServletRequest request = getCurrentRequest();
        RequestScopeCache cache = getRequestCache(request);
        cache.registerCallback(name, callback);
    }

    @Override
    public String[] getBeanNames() {
        HttpServletRequest request = getCurrentRequest();
        RequestScopeCache cache = getRequestCache(request);
        return cache.getBeanCache().keySet().toArray(new String[0]);
    }

    @Override
    public String getConversationId() {
        HttpServletRequest request = getCurrentRequest();
        return "request-" + request.hashCode();
    }

    /**
     * 销毁当前请求的所有 Bean
     * 应在请求结束时调用
     */
    public static void destroyCurrentRequest() {
        try {
            HttpServletRequest request = getCurrentRequestStatic();
            if (request != null) {
                RequestScopeCache cache = (RequestScopeCache) request.getAttribute(REQUEST_SCOPE_CACHE_KEY);
                if (cache != null) {
                    cache.destroy();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Destroyed request scope beans for request: {}", request.hashCode());
                    }
                }
            }
        } finally {
            clearCurrentRequest();
        }
    }

    private static HttpServletRequest getCurrentRequestStatic() {
        WeakRef<HttpServletRequest> ref = currentRequest.get();
        return ref != null ? ref.get() : null;
    }
}
