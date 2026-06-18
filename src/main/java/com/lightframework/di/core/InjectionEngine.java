package com.lightframework.di.core;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Lazy;
import com.lightframework.di.annotation.Qualifier;
import com.lightframework.di.annotation.Resource;
import com.lightframework.di.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 注入引擎 — 极致优化版。
 *
 * 优化策略：
 * 1. 消除热路径反射：所有注解信息在 InjectionMetadata 中预计算，注入时零 getAnnotation 调用
 * 2. 消除枚举 switch：每种注入类型有独立方法，直接调用
 * 3. 内联热路径：resolve → inject 合并到单循环，减少方法调用栈
 * 4. 使用 Set 跟踪已注入字段，跨注解类型去重（同一字段同时有 @Resource 和 @Autowired 时不重复注入）
 * 5. Logger 检查前置：debug 日志在循环外判断，不在热路径中
 */
public class InjectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(InjectionEngine.class);

    private final DependencyContainer container;
    private final DefaultTypeConverter typeConverter;
    private final PlaceholderResolver placeholderResolver;

    public InjectionEngine(DependencyContainer container,
                           DefaultTypeConverter typeConverter,
                           PlaceholderResolver placeholderResolver) {
        this.container = container;
        this.typeConverter = typeConverter;
        this.placeholderResolver = placeholderResolver;
    }

    /**
     * ★ 极致优化注入入口 — 使用 Set 跟踪已注入字段/方法，避免 BitSet 索引映射和跨注解冲突。
     */
    public void injectAll(String beanName, Object bean,
                          InjectionMetadata metadata) throws Exception {
        injectResourceFields(beanName, bean, metadata);
        injectResourceMethods(beanName, bean, metadata);
        injectAutowiredFields(beanName, bean, metadata);
        injectAutowiredMethods(beanName, bean, metadata);
        if (placeholderResolver != null) {
            injectValueFields(beanName, bean, metadata);
        }
    }

    /**
     * ★ @Resource 字段注入 — 内联解析 + 注入，零额外方法调用。
     * 使用 metadata.isFieldInjected() 跨注解去重。
     */
    private void injectResourceFields(String beanName, Object bean,
                                       InjectionMetadata metadata) throws Exception {
        int size = metadata.resourceFields.size();
        if (size == 0) return;

        boolean debugEnabled = logger.isDebugEnabled();

        for (int i = 0; i < size; i++) {
            AnnotationInjectEntry entry = metadata.resourceEntries.get(i);
            if (entry == null) continue;

            // ★ 跨注解去重：如果该字段已被 @Autowired 注入过，跳过
            if (metadata.isFieldInjected(entry.field)) continue;

            Object dependency;
            if (entry.lazyProxy != null) {
                dependency = entry.lazyProxy;
            } else {
                dependency = resolveResourceByNameOrType(beanName, entry.resourceName, entry.type);
            }

            if (dependency != null) {
                entry.injector.inject(bean, dependency);
                metadata.markFieldInjected(entry.field);
                if (debugEnabled) {
                    logger.debug("@Resource injected field {} in bean {}", entry.fieldName, beanName);
                }
            } else {
                throw new DependencyResolutionException(beanName,
                    "Required dependency not found for @Resource field: " + entry.fieldName +
                    " (name='" + entry.resourceName + "', type=" + entry.type.getSimpleName() + ")");
            }
        }
    }

    /**
     * ★ @Autowired 字段注入 — 内联解析 + 注入，支持泛型类型解析。
     */
    private void injectAutowiredFields(String beanName, Object bean,
                                        InjectionMetadata metadata) throws Exception {
        int size = metadata.autowiredFields.size();
        if (size == 0) return;

        boolean debugEnabled = logger.isDebugEnabled();

        for (int i = 0; i < size; i++) {
            AnnotationInjectEntry entry = metadata.autowiredEntries.get(i);
            if (entry == null) continue;

            // ★ 跨注解去重：如果该字段已被 @Resource 注入过，跳过
            if (metadata.isFieldInjected(entry.field)) continue;

            Object dependency;
            if (entry.lazyProxy != null) {
                dependency = entry.lazyProxy;
            } else {
                dependency = container.resolveDependencyWithGenerics(entry.field, entry.qualifier, entry.required);
            }

            if (dependency != null) {
                entry.injector.inject(bean, dependency);
                metadata.markFieldInjected(entry.field);
                if (debugEnabled) {
                    logger.debug("@Autowired injected field {} in bean {}", entry.fieldName, beanName);
                }
            } else if (entry.required) {
                throw new DependencyResolutionException(beanName,
                    "Required dependency not found for @Autowired field: " + entry.fieldName);
            }
        }
    }

    /**
     * ★ @Resource 方法注入 — 内联解析 + 注入。
     */
    private void injectResourceMethods(String beanName, Object bean,
                                        InjectionMetadata metadata) throws Exception {
        int size = metadata.resourceMethods.size();
        if (size == 0) return;

        boolean debugEnabled = logger.isDebugEnabled();

        for (int i = 0; i < size; i++) {
            Method method = metadata.resourceMethods.get(i);
            if (method.getParameterCount() != 1) continue;

            // ★ 跨注解去重
            if (metadata.isMethodInjected(method)) continue;

            // ★ 预计算的资源名称和类型
            String name = metadata.resourceEntries.get(i) != null ?
                         metadata.resourceEntries.get(i).resourceName : null;
            Class<?> type = metadata.resourceEntries.get(i) != null ?
                          metadata.resourceEntries.get(i).type : method.getParameterTypes()[0];

            Object dependency = resolveResourceByNameOrType(beanName, name, type);
            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                metadata.markMethodInjected(method);
                if (debugEnabled) {
                    logger.debug("@Resource injected method {} in bean {}", method.getName(), beanName);
                }
            } else {
                throw new DependencyResolutionException(beanName,
                    "Required dependency not found for @Resource method: " + method.getName());
            }
        }
    }

    /**
     * ★ @Autowired 方法注入 — 内联解析 + 注入。
     */
    private void injectAutowiredMethods(String beanName, Object bean,
                                         InjectionMetadata metadata) throws Exception {
        int size = metadata.autowiredMethods.size();
        if (size == 0) return;

        boolean debugEnabled = logger.isDebugEnabled();

        for (int i = 0; i < size; i++) {
            Method method = metadata.autowiredMethods.get(i);
            if (method.getParameterCount() != 1) continue;

            // ★ 跨注解去重
            if (metadata.isMethodInjected(method)) continue;

            Class<?> paramType = method.getParameterTypes()[0];
            AnnotationInjectEntry entry = metadata.autowiredEntries.size() > i ?
                                         metadata.autowiredEntries.get(i) : null;
            String qualifier = entry != null ? entry.qualifier : null;
            boolean required = entry != null ? entry.required : true;

            Object dependency = container.resolveDependency(paramType, method.getName(), qualifier, required);
            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                metadata.markMethodInjected(method);
                if (debugEnabled) {
                    logger.debug("@Autowired setter {} in bean {}", method.getName(), beanName);
                }
            } else if (required) {
                throw new DependencyResolutionException(beanName,
                    "Required dependency not found for @Autowired method: " + method.getName());
            }
        }
    }

    /**
     * ★ @Value 字段注入 — 预计算注解值，消除运行时 getAnnotation。
     */
    private void injectValueFields(String beanName, Object bean,
                                    InjectionMetadata metadata) throws Exception {
        int size = metadata.valueFields.size();
        if (size == 0) return;

        boolean debugEnabled = logger.isDebugEnabled();

        for (int i = 0; i < size; i++) {
            AnnotationInjectEntry entry = metadata.valueEntries.get(i);
            if (entry == null) continue;

            // ★ 跨注解去重
            if (metadata.isFieldInjected(entry.field)) continue;

            String placeholder = entry.placeholder;
            if (placeholder == null || placeholder.isEmpty()) {
                throw new DependencyResolutionException(beanName,
                    "@Value annotation on field '" + entry.fieldName + "' has an empty value");
            }

            String resolvedValue = placeholderResolver.resolvePlaceholder(placeholder);
            Object convertedValue = typeConverter.convert(resolvedValue, entry.type);

            entry.injector.inject(bean, convertedValue);
            metadata.markFieldInjected(entry.field);
            if (debugEnabled) {
                logger.debug("@Value injected field {} with value '{}' in bean {}",
                    entry.fieldName, resolvedValue, beanName);
            }
        }
    }

    /**
     * ★ @Resource 依赖解析 — 内联逻辑，无额外方法调用。
     * 优先按 name 查找，回退到按 type 查找。
     */
    private Object resolveResourceByNameOrType(String beanName, String name, Class<?> type) throws Exception {
        if (name != null && container.containsBean(name)) {
            return container.getBean(name, type);
        }
        return container.getBean(type);
    }
}
