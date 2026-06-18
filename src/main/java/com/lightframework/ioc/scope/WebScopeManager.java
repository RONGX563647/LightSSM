package com.lightframework.ioc.scope;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Web 作用域生命周期管理器
 * 负责管理 Request、Session、Application 作用域的完整生命周期
 * 
 * 核心功能：
 * 1. 通过 Filter 自动管理 Request 作用域的创建和销毁
 * 2. 通过 HttpSessionListener 自动管理 Session 作用域的销毁
 * 3. 通过 ServletContextListener 管理应用级作用域的初始化/销毁
 * 4. 提供编程式 API 用于手动管理作用域
 * 
 * 使用方式：
 * - Web 应用：在 web.xml 或代码中注册 WebScopeFilter
 * - 编程式：直接调用 requestStarted()/requestEnded() 等方法
 */
public class WebScopeManager {

    private static final Logger logger = LoggerFactory.getLogger(WebScopeManager.class);

    private static volatile WebScopeManager instance;

    private final ScopeRegistry scopeRegistry;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * 会话监听器映射（用于 Session 销毁时回调）
     */
    private final Map<String, SessionScope> sessionScopes = new ConcurrentHashMap<>();

    private WebScopeManager() {
        this.scopeRegistry = new ScopeRegistry();
    }

    /**
     * 获取全局单例实例
     */
    public static WebScopeManager getInstance() {
        if (instance == null) {
            synchronized (WebScopeManager.class) {
                if (instance == null) {
                    instance = new WebScopeManager();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化 WebScopeManager
     * 应在应用启动时调用
     */
    public void initialize() {
        if (initialized.compareAndSet(false, true)) {
            logger.info("WebScopeManager initialized with {} built-in scopes", scopeRegistry.getScopeCount());
        }
    }

    /**
     * 销毁 WebScopeManager 及其管理的所有作用域
     * 应在应用关闭时调用
     */
    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            logger.info("Destroying WebScopeManager and all managed scopes...");

            // 销毁应用级作用域
            scopeRegistry.getApplicationScope().destroyAll();

            // 清理会话监听器
            sessionScopes.clear();

            // 销毁所有自定义作用域
            scopeRegistry.destroyAll();

            // 清除 ThreadLocal
            RequestScope.clearCurrentRequest();
            SessionScope.clearCurrentRequest();

            logger.info("WebScopeManager destroyed successfully");
        }
    }

    /**
     * 获取作用域注册表
     */
    public ScopeRegistry getScopeRegistry() {
        return scopeRegistry;
    }

    /**
     * 注册自定义作用域
     */
    public void registerScope(String scopeName, Scope scope) {
        checkNotDestroyed();
        scopeRegistry.registerScope(scopeName, scope);
    }

    /**
     * 根据名称获取作用域
     */
    public Scope getScope(String scopeName) {
        return scopeRegistry.getScope(scopeName);
    }

    // ==================== Request 生命周期管理 ====================

    /**
     * 标记请求开始
     * 应在 Filter 的 doFilter 之前调用
     */
    public void requestStarted(HttpServletRequest request) {
        checkNotDestroyed();
        RequestScope.setCurrentRequest(request);
        SessionScope.setCurrentRequest(request);
        if (logger.isTraceEnabled()) {
            logger.trace("Request scope started for request: {}", request.hashCode());
        }
    }

    /**
     * 标记请求结束
     * 应在 Filter 的 doFilter 之后调用
     */
    public void requestEnded() {
        try {
            RequestScope.destroyCurrentRequest();
            if (logger.isTraceEnabled()) {
                logger.trace("Request scope ended");
            }
        } finally {
            RequestScope.clearCurrentRequest();
            SessionScope.clearCurrentRequest();
        }
    }

    // ==================== Session 生命周期管理 ====================

    /**
     * Session 创建回调
     * 应在 HttpSessionListener 中调用
     */
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        sessionScopes.put(session.getId(), new SessionScope());
        logger.debug("Session created: {}", session.getId());
    }

    /**
     * Session 销毁回调
     * 应在 HttpSessionListener 中调用
     */
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        String sessionId = session.getId();

        // 销毁 Session 作用域 Bean
        SessionScope.destroySessionScope(sessionId, session);
        sessionScopes.remove(sessionId);

        logger.debug("Session destroyed: {}", sessionId);
    }

    // ==================== ServletContext 生命周期管理 ====================

    /**
     * 应用启动回调
     * 应在 ServletContextListener 中调用
     */
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        context.setAttribute(WebScopeManager.class.getName(), this);
        initialize();
        logger.info("WebScopeManager context initialized for application: {}", context.getContextPath());
    }

    /**
     * 应用关闭回调
     * 应在 ServletContextListener 中调用
     */
    public void contextDestroyed(ServletContextEvent event) {
        destroy();
        logger.info("WebScopeManager context destroyed for application");
    }

    private void checkNotDestroyed() {
        if (destroyed.get()) {
            throw new IllegalStateException("WebScopeManager has been destroyed");
        }
    }

    // ==================== Web Filter ====================

    /**
     * 作用域管理 Filter
     * 自动管理 Request 和 Session 作用域的生命周期
     * 
     * 配置示例（web.xml）：
     * <pre>
     * &lt;filter&gt;
     *     &lt;filter-name&gt;webScopeFilter&lt;/filter-name&gt;
     *     &lt;filter-class&gt;com.lightframework.ioc.scope.WebScopeManager$WebScopeFilter&lt;/filter-class&gt;
     * &lt;/filter&gt;
     * &lt;filter-mapping&gt;
     *     &lt;filter-name&gt;webScopeFilter&lt;/filter-name&gt;
     *     &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
     * &lt;/filter-mapping&gt;
     * </pre>
     */
    public static class WebScopeFilter implements Filter {

        private WebScopeManager scopeManager;

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            this.scopeManager = WebScopeManager.getInstance();
            if (!this.scopeManager.initialized.get()) {
                this.scopeManager.initialize();
            }
            logger.info("WebScopeFilter initialized");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (!(request instanceof HttpServletRequest)) {
                chain.doFilter(request, response);
                return;
            }

            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // 请求开始：绑定上下文
            scopeManager.requestStarted(httpRequest);

            try {
                chain.doFilter(request, response);
            } finally {
                // 请求结束：清理作用域
                scopeManager.requestEnded();
            }
        }

        @Override
        public void destroy() {
            logger.info("WebScopeFilter destroyed");
        }
    }

    // ==================== HttpSessionListener ====================

    /**
     * Session 生命周期监听器
     * 自动管理 Session 作用域的创建和销毁
     * 
     * 配置示例（web.xml）：
     * <pre>
     * &lt;listener&gt;
     *     &lt;listener-class&gt;com.lightframework.ioc.scope.WebScopeManager$SessionLifecycleListener&lt;/listener-class&gt;
     * &lt;/listener&gt;
     * </pre>
     */
    public static class SessionLifecycleListener implements HttpSessionListener {

        private WebScopeManager scopeManager;

        @Override
        public void sessionCreated(HttpSessionEvent se) {
            scopeManager = WebScopeManager.getInstance();
            scopeManager.sessionCreated(se);
        }

        @Override
        public void sessionDestroyed(HttpSessionEvent se) {
            if (scopeManager != null) {
                scopeManager.sessionDestroyed(se);
            } else {
                // 降级处理：直接销毁
                SessionScope.destroySessionScope(se.getSession().getId(), se.getSession());
            }
        }
    }

    // ==================== ServletContextListener ====================

    /**
     * 应用生命周期监听器
     * 自动管理应用级作用域的初始化和销毁
     * 
     * 配置示例（web.xml）：
     * <pre>
     * &lt;listener&gt;
     *     &lt;listener-class&gt;com.lightframework.ioc.scope.WebScopeManager$ContextLifecycleListener&lt;/listener-class&gt;
     * &lt;/listener&gt;
     * </pre>
     */
    public static class ContextLifecycleListener implements ServletContextListener {

        @Override
        public void contextInitialized(ServletContextEvent sce) {
            WebScopeManager.getInstance().contextInitialized(sce);
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            WebScopeManager.getInstance().contextDestroyed(sce);
        }
    }
}
