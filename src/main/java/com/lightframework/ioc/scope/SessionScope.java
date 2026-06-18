package com.lightframework.ioc.scope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 会话作用域实现
 * 每个 HTTP Session 拥有独立的 Bean 实例，Session 过期或失效时自动销毁
 * 
 * 性能优化：
 * 1. 使用 HttpSession 属性作为主存储，自动跟随 Session 生命周期
 * 2. 使用 ConcurrentHashMap 作为缓存，避免同步开销
 * 3. 实现 HttpSessionBindingListener 自动清理
 * 4. 延迟初始化，仅在首次访问时创建 Bean
 */
public class SessionScope implements Scope {

    private static final Logger logger = LoggerFactory.getLogger(SessionScope.class);

    public static final String SCOPE_NAME = "session";

    /**
     * Session 属性中存储缓存的键
     */
    private static final String SESSION_SCOPE_CACHE_KEY = SessionScope.class.getName() + ".CACHE";

    /**
     * ThreadLocal 持有当前请求的引用
     */
    private static final ThreadLocal<RequestScope.WeakRef<HttpServletRequest>> currentRequest = new ThreadLocal<>();

    /**
     * 全局 Bean 元数据缓存（跨 Session 共享）
     * 用于记录哪些 Bean 需要销毁回调
     */
    private final Map<String, AtomicBoolean> registeredCallbacks = new ConcurrentHashMap<>(32);

    /**
     * Session 级缓存容器
     */
    static class SessionScopeCache implements HttpSessionBindingListener {
        private final Map<String, Object> beanCache = new ConcurrentHashMap<>(16);
        private final Map<String, Runnable> destructionCallbacks = new ConcurrentHashMap<>(8);
        private final String sessionId;

        SessionScopeCache(String sessionId) {
            this.sessionId = sessionId;
        }

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

        Map<String, Object> getBeanCache() {
            return beanCache;
        }

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            if (logger.isDebugEnabled()) {
                logger.debug("Session scope cache bound to session: {}", sessionId);
            }
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            destroy();
        }

        /**
         * 执行所有销毁回调并清空缓存
         */
        void destroy() {
            destructionCallbacks.forEach((name, callback) -> {
                try {
                    callback.run();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Executed destruction callback for bean '{}' in session scope", name);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to execute destruction callback for bean '{}' in session scope: {}", name, e.getMessage());
                }
            });
            beanCache.clear();
            destructionCallbacks.clear();
            if (logger.isDebugEnabled()) {
                logger.debug("Destroyed session scope beans for session: {}", sessionId);
            }
        }
    }

    /**
     * 设置当前请求上下文
     */
    public static void setCurrentRequest(HttpServletRequest request) {
        currentRequest.set(new RequestScope.WeakRef<>(request));
    }

    /**
     * 清除当前请求上下文
     */
    public static void clearCurrentRequest() {
        currentRequest.remove();
    }

    /**
     * 获取当前 Session
     */
    private HttpSession getCurrentSession() {
        HttpServletRequest request = getCurrentRequestInternal();
        if (request == null) {
            throw new IllegalStateException("No request bound to current thread. " +
                    "Ensure SessionScope.setCurrentRequest() is called before accessing session-scoped beans.");
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("No HttpSession available. " +
                    "Session-scoped beans require an active HTTP session.");
        }
        return session;
    }

    /**
     * 获取或创建 Session（用于写入）
     */
    private HttpSession getSessionForWrite() {
        HttpServletRequest request = getCurrentRequestInternal();
        if (request == null) {
            throw new IllegalStateException("No request bound to current thread.");
        }
        return request.getSession(); // 创建新 Session 如果不存在
    }

    private HttpServletRequest getCurrentRequestInternal() {
        RequestScope.WeakRef<HttpServletRequest> ref = currentRequest.get();
        return ref != null ? ref.get() : null;
    }

    /**
     * 获取或创建 Session 级缓存
     */
    @SuppressWarnings("unchecked")
    private SessionScopeCache getSessionCache(HttpSession session) {
        SessionScopeCache cache = (SessionScopeCache) session.getAttribute(SESSION_SCOPE_CACHE_KEY);
        if (cache == null) {
            cache = new SessionScopeCache(session.getId());
            session.setAttribute(SESSION_SCOPE_CACHE_KEY, cache);
            if (logger.isDebugEnabled()) {
                logger.debug("Created new session scope cache for session: {}", session.getId());
            }
        }
        return cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object get(String name, ObjectFactory<?> objectFactory) {
        HttpSession session = getSessionForWrite();
        SessionScopeCache cache = getSessionCache(session);

        // 快速路径
        Object bean = cache.getBean(name);
        if (bean != null) {
            return bean;
        }

        // 同步创建
        synchronized (this) {
            bean = cache.getBean(name);
            if (bean == null) {
                try {
                    bean = objectFactory.getObject();
                    cache.putBean(name, bean);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created new session-scoped bean: '{}' in session: {}", name, session.getId());
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create session-scoped bean '" + name + "'", e);
                }
            }
        }

        return bean;
    }

    @Override
    public Object remove(String name) {
        HttpSession session = getCurrentSession();
        SessionScopeCache cache = getSessionCache(session);
        Object removed = cache.removeBean(name);
        if (logger.isDebugEnabled() && removed != null) {
            logger.debug("Removed session-scoped bean: '{}' from session: {}", name, session.getId());
        }
        return removed;
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        if (callback == null) {
            throw new IllegalArgumentException("Destruction callback must not be null");
        }
        HttpSession session = getSessionForWrite();
        SessionScopeCache cache = getSessionCache(session);
        cache.registerCallback(name, callback);
    }

    @Override
    public String[] getBeanNames() {
        HttpSession session = getCurrentSession();
        SessionScopeCache cache = getSessionCache(session);
        return cache.getBeanCache().keySet().toArray(new String[0]);
    }

    @Override
    public String getConversationId() {
        HttpSession session = getCurrentSession();
        return "session-" + session.getId();
    }

    /**
     * 手动销毁指定 Session 的作用域 Bean
     * 用于 Session 失效监听器中调用
     */
    public static void destroySessionScope(String sessionId, HttpSession session) {
        if (session != null) {
            SessionScopeCache cache = (SessionScopeCache) session.getAttribute(SESSION_SCOPE_CACHE_KEY);
            if (cache != null) {
                cache.destroy();
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Destroyed session scope for session: {}", sessionId);
        }
    }
}
