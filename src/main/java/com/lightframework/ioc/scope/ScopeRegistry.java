package com.lightframework.ioc.scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 作用域注册表
 * 用于管理和注册自定义作用域，支持运行时动态添加/移除作用域
 * 
 * 线程安全：使用 ConcurrentHashMap 支持并发访问
 * 性能优化：读操作无锁，写操作通过 ConcurrentHashMap 保证一致性
 */
public class ScopeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ScopeRegistry.class);

    /**
     * 内置作用域常量
     */
    public static final String SINGLETON = "singleton";
    public static final String PROTOTYPE = "prototype";
    public static final String REQUEST = "request";
    public static final String SESSION = "session";
    public static final String APPLICATION = "application";

    /**
     * 作用域注册表（线程安全）
     * Key: 作用域名称，Value: Scope 实例
     */
    private final Map<String, Scope> scopeMap = new ConcurrentHashMap<>(32);

    /**
     * 默认的单例和作用域实例（共享）
     */
    private final ApplicationScope applicationScope = new ApplicationScope();

    public ScopeRegistry() {
        // 注册内置 Web 作用域
        registerScope(REQUEST, new RequestScope());
        registerScope(SESSION, new SessionScope());
        registerScope(APPLICATION, applicationScope);
        logger.debug("Built-in scopes registered: {}, {}, {}", REQUEST, SESSION, APPLICATION);
    }

    /**
     * 注册自定义作用域
     * 如果同名作用域已存在，将被替换
     *
     * @param scopeName 作用域名称
     * @param scope Scope 实例
     * @throws IllegalArgumentException 如果 scopeName 或 scope 为 null
     */
    public void registerScope(String scopeName, Scope scope) {
        if (scopeName == null || scopeName.isEmpty()) {
            throw new IllegalArgumentException("Scope name must not be null or empty");
        }
        if (scope == null) {
            throw new IllegalArgumentException("Scope must not be null");
        }

        Scope previous = scopeMap.put(scopeName, scope);
        if (previous != null) {
            logger.info("Replaced existing scope '{}' (was: {})", scopeName, previous.getClass().getSimpleName());
        } else {
            logger.debug("Registered new scope: '{}'", scopeName);
        }
    }

    /**
     * 获取指定名称的作用域
     *
     * @param scopeName 作用域名称
     * @return Scope 实例，如果未注册则返回 null
     */
    public Scope getScope(String scopeName) {
        return scopeMap.get(scopeName);
    }

    /**
     * 检查作用域是否已注册
     *
     * @param scopeName 作用域名称
     * @return true 如果已注册
     */
    public boolean hasScope(String scopeName) {
        return scopeMap.containsKey(scopeName);
    }

    /**
     * 移除指定作用域
     * 注意：不建议移除内置作用域
     *
     * @param scopeName 作用域名称
     * @return 被移除的 Scope 实例，如果不存在则返回 null
     */
    public Scope removeScope(String scopeName) {
        Scope removed = scopeMap.remove(scopeName);
        if (removed != null) {
            logger.debug("Removed scope: '{}'", scopeName);
        }
        return removed;
    }

    /**
     * 获取所有已注册的作用域名称（只读）
     *
     * @return 作用域名称集合
     */
    public java.util.Set<String> getScopeNames() {
        return Collections.unmodifiableSet(scopeMap.keySet());
    }

    /**
     * 获取所有已注册的作用域（只读快照）
     *
     * @return 作用域映射的不可变视图
     */
    public Map<String, Scope> getScopes() {
        return Collections.unmodifiableMap(new HashMap<>(scopeMap));
    }

    /**
     * 获取应用级作用域实例
     */
    public ApplicationScope getApplicationScope() {
        return applicationScope;
    }

    /**
     * 销毁所有自定义作用域（不清理内置作用域）
     */
    public void destroyCustomScopes() {
        scopeMap.keySet().removeIf(scopeName ->
                !SINGLETON.equals(scopeName) &&
                !PROTOTYPE.equals(scopeName) &&
                !REQUEST.equals(scopeName) &&
                !SESSION.equals(scopeName) &&
                !APPLICATION.equals(scopeName)
        );
        logger.info("All custom scopes destroyed");
    }

    /**
     * 销毁所有作用域（包括内置）
     * 应在应用关闭时调用
     */
    public void destroyAll() {
        // 先销毁应用级作用域
        applicationScope.destroyAll();

        // 清空所有注册
        scopeMap.clear();

        logger.info("All scopes destroyed");
    }

    /**
     * 获取已注册的作用域数量
     */
    public int getScopeCount() {
        return scopeMap.size();
    }

    @Override
    public String toString() {
        return "ScopeRegistry{scopes=" + scopeMap.keySet() + "}";
    }
}
