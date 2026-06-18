package com.lightframework.ioc.core;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Lazy;
import com.lightframework.ioc.annotation.Qualifier;
import com.lightframework.ioc.annotation.Resource;
import com.lightframework.ioc.annotation.Value;
import com.lightframework.ioc.exception.BeanCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

/**
 * 统一注入引擎 — 将 @Resource、@Autowired、@Value 的字段/方法注入逻辑统一处理，
 * 消除 DefaultListableBeanFactory 中 4 个 injectXxx 方法的重复代码。
 */
public class InjectionEngine {

    private static final Logger logger = LoggerFactory.getLogger(InjectionEngine.class);

    private final DefaultListableBeanFactory beanFactory;
    private final DefaultTypeConverter typeConverter;
    private final PropertyPlaceholderConfigurer propertyPlaceholderConfigurer;

    public InjectionEngine(DefaultListableBeanFactory beanFactory,
                           DefaultTypeConverter typeConverter,
                           PropertyPlaceholderConfigurer propertyPlaceholderConfigurer) {
        this.beanFactory = beanFactory;
        this.typeConverter = typeConverter;
        this.propertyPlaceholderConfigurer = propertyPlaceholderConfigurer;
    }

    /**
     * 统一注入入口：按顺序执行 @Resource → @Autowired → @Value
     */
    public void injectAll(String beanName, Object bean,
                         AnnotationMetadata metadata,
                         Map<Object, Integer> fieldIdx,
                         BitSet injectedFieldBits,
                         BitSet injectedMethodBits) throws Exception {
        // 第一轮: @Resource 字段注入
        injectEntries(beanName, bean, metadata.resourceFields, metadata.resourceEntries,
                      fieldIdx, injectedFieldBits, InjectionType.RESOURCE_FIELD);
        // 第二轮: @Resource 方法注入
        injectMethodEntries(beanName, bean, metadata.resourceMethods, fieldIdx, injectedMethodBits,
                            InjectionType.RESOURCE_METHOD);
        // 第三轮: @Autowired 字段注入
        injectEntries(beanName, bean, metadata.autowiredFields, metadata.autowiredEntries,
                      fieldIdx, injectedFieldBits, InjectionType.AUTOWIRED_FIELD);
        // 第四轮: @Autowired 方法注入
        injectMethodEntries(beanName, bean, metadata.autowiredMethods, fieldIdx, injectedMethodBits,
                            InjectionType.AUTOWIRED_METHOD);
        // 第五轮: @Value 字段注入 (如果有占位符配置器)
        if (propertyPlaceholderConfigurer != null) {
            injectValueEntries(beanName, bean, metadata.valueFields, metadata.valueEntries,
                               fieldIdx, injectedFieldBits);
        }
    }

    /**
     * 统一字段注入方法 — 处理 @Resource 或 @Autowired 字段
     */
    private void injectEntries(String beanName, Object bean,
                               List<Field> fields,
                               List<AnnotationInjectEntry> entries,
                               Map<Object, Integer> fieldIdx,
                               BitSet injectedBits,
                               InjectionType type) throws Exception {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            AnnotationInjectEntry entry = i < entries.size() ? entries.get(i) : null;

            // 检查是否已被 @Resource 注入过（防止重复）
            Integer bit = fieldIdx.get(field);
            if (bit != null && injectedBits.get(bit)) continue;

            Object dependency = resolveDependencyForField(beanName, field, type);
            if (dependency != null) {
                if (entry != null) entry.injector.inject(bean, dependency);
                else { field.setAccessible(true); field.set(bean, dependency); }
                if (bit != null) injectedBits.set(bit);
                if (logger.isDebugEnabled()) {
                    logger.debug("{} injected field {} in bean {}", type.label, field.getName(), beanName);
                }
            } else if (type.isRequired()) {
                throw new BeanCreationException(beanName,
                    "Required dependency not found for " + type.label + " field: " + field.getName());
            }
        }
    }

    /**
     * 统一方法注入方法 — 处理 @Resource 或 @Autowired setter 方法
     */
    private void injectMethodEntries(String beanName, Object bean,
                                     List<Method> methods,
                                     Map<Object, Integer> fieldIdx,
                                     BitSet injectedBits,
                                     InjectionType type) throws Exception {
        for (Method method : methods) {
            Integer bit = fieldIdx.get(method);
            if (bit != null && injectedBits.get(bit)) continue;

            if (method.getParameterCount() != 1) continue;

            Object dependency = resolveDependencyForMethod(beanName, method, type);
            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                if (bit != null) injectedBits.set(bit);
                if (logger.isDebugEnabled()) {
                    logger.debug("{} injected method {} in bean {}", type.label, method.getName(), beanName);
                }
            } else if (type.isRequired()) {
                throw new BeanCreationException(beanName,
                    "Required dependency not found for " + type.label + " method: " + method.getName());
            }
        }
    }

    /**
     * @Value 字段注入（需要占位符解析和类型转换）
     */
    private void injectValueEntries(String beanName, Object bean,
                                    List<Field> fields,
                                    List<AnnotationInjectEntry> entries,
                                    Map<Object, Integer> fieldIdx,
                                    BitSet injectedBits) throws Exception {
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            AnnotationInjectEntry entry = i < entries.size() ? entries.get(i) : null;
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation == null) continue;

            String placeholder = valueAnnotation.value();
            if (placeholder.isEmpty()) {
                throw new BeanCreationException(beanName,
                    "@Value annotation on field '" + field.getName() + "' has an empty value");
            }

            String resolvedValue = propertyPlaceholderConfigurer.resolvePlaceholder(placeholder);
            Object convertedValue = typeConverter.convert(resolvedValue, field.getType());

            if (entry != null) {
                entry.injector.inject(bean, convertedValue);
            } else {
                field.setAccessible(true);
                field.set(bean, convertedValue);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("@Value injected field {} with value '{}' in bean {}",
                    field.getName(), resolvedValue, beanName);
            }
        }
    }

    /**
     * 根据注入类型解析字段依赖
     */
    private Object resolveDependencyForField(String beanName, Field field, InjectionType type) throws Exception {
        switch (type) {
            case RESOURCE_FIELD:
                return resolveResourceDependency(beanName, field);
            case AUTOWIRED_FIELD:
                return resolveAutowiredDependency(beanName, field);
            default:
                logger.warn("Unsupported injection type for field resolution: {}", type);
                return null;
        }
    }

    /**
     * 根据注入类型解析方法依赖
     */
    private Object resolveDependencyForMethod(String beanName, Method method, InjectionType type) throws Exception {
        switch (type) {
            case RESOURCE_METHOD:
                return resolveResourceDependencyForMethod(beanName, method);
            case AUTOWIRED_METHOD:
                return resolveAutowiredDependencyForMethod(beanName, method);
            default:
                logger.warn("Unsupported injection type for method resolution: {}", type);
                return null;
        }
    }

    /**
     * 解析 @Resource 依赖（按 name 优先，然后按 type）
     */
    private Object resolveResourceDependency(String beanName, Field field) throws Exception {
        Resource resource = field.getAnnotation(Resource.class);
        if (resource == null) return null;

        Lazy lazy = field.getAnnotation(Lazy.class);
        if (lazy != null && lazy.value()) {
            String name = resource.name().isEmpty() ? field.getName() : resource.name();
            Class<?> type = resource.type() == Object.class ? field.getType() : resource.type();
            Object lazyProxy = beanFactory.createLazyProxy(type, name);
            if (lazyProxy != null) return lazyProxy;
        }

        String name = resource.name().isEmpty() ? field.getName() : resource.name();
        Class<?> type = resource.type() == Object.class ? field.getType() : resource.type();

        if (beanFactory.containsBean(name)) {
            return beanFactory.getBean(name, type);
        }
        return beanFactory.getBean(type);
    }

    /**
     * 解析 @Resource 方法依赖
     */
    private Object resolveResourceDependencyForMethod(String beanName, Method method) throws Exception {
        Resource resource = method.getAnnotation(Resource.class);
        if (resource == null) return null;

        String name = resource.name();
        if (name.isEmpty()) {
            name = decapitalize(method.getName().startsWith("set") ?
                method.getName().substring(3) : method.getName());
        }
        Class<?> type = resource.type() == Object.class ?
            method.getParameterTypes()[0] : resource.type();

        if (beanFactory.containsBean(name)) {
            return beanFactory.getBean(name, type);
        }
        return beanFactory.getBean(type);
    }

    /**
     * 解析 @Autowired 字段依赖（支持泛型和 @Lazy）
     */
    private Object resolveAutowiredDependency(String beanName, Field field) throws Exception {
        Lazy lazy = field.getAnnotation(Lazy.class);
        String qualifier = extractQualifier(field);
        Autowired autowired = field.getAnnotation(Autowired.class);

        if (lazy != null && lazy.value()) {
            Object lazyProxy = beanFactory.createLazyProxy(field.getType(), qualifier);
            if (lazyProxy != null) return lazyProxy;
        }

        return beanFactory.resolveDependencyWithGenerics(field, qualifier,
            autowired != null ? autowired.required() : true);
    }

    /**
     * 解析 @Autowired 方法依赖
     */
    private Object resolveAutowiredDependencyForMethod(String beanName, Method method) throws Exception {
        Class<?> paramType = method.getParameterTypes()[0];
        String qualifier = extractQualifier(method);
        if (qualifier == null && method.getParameterAnnotations().length > 0) {
            for (java.lang.annotation.Annotation ann : method.getParameterAnnotations()[0]) {
                if (ann instanceof Qualifier) {
                    qualifier = ((Qualifier) ann).value();
                    break;
                }
            }
        }
        Autowired autowired = method.getAnnotation(Autowired.class);
        return beanFactory.resolveDependency(paramType, method.getName(), qualifier,
            autowired != null ? autowired.required() : true);
    }

    /**
     * 从注解元素中提取 qualifier
     */
    private String extractQualifier(java.lang.reflect.AnnotatedElement element) {
        Qualifier qualifier = element.getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) {
            return qualifier.value();
        }
        return null;
    }

    /**
     * 首字母小写
     */
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * 注入类型枚举
     */
    enum InjectionType {
        RESOURCE_FIELD("@Resource", true),
        RESOURCE_METHOD("@Resource", true),
        AUTOWIRED_FIELD("@Autowired", false),
        AUTOWIRED_METHOD("@Autowired", false);

        final String label;
        final boolean resource;

        InjectionType(String label, boolean resource) {
            this.label = label;
            this.resource = resource;
        }

        boolean isRequired() {
            return resource;
        }
    }
}
