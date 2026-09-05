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
// TODO [L3][优化-模板方法] injectResourceFields / injectAutowiredFields / injectResourceMethods / injectAutowiredMethods / injectValueFields 五个私有方法结构高度雷同（取列表→跨注解去重→解析依赖→注入→标记→可选 debug 日志）。可抽出抽象基类 TemplateInjector，把「取条目 / 解析依赖 / 执行注入」作为抽象步骤，子类只实现差异，新增注入类型时零改动主流程。；写对标志：按模板方法模式完成实现，新增单测覆盖“钩子方法被回调、算法骨架固定”的主路径与一条异常路径，断言执行顺序与结果正确。
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
        // TODO [L3][优化-观察者模式] 注入过程缺少生命周期钩子：可在「开始注入 / 每个字段注入成功 / 注入失败」时发布事件（如 InjectionEvent），让日志、监控、懒代理预热等作为监听器订阅，避免横切逻辑侵入主流程。；写对标志：按观察者模式完成实现，新增单测覆盖“主题状态变更后所有观察者被通知”的主路径与一条注销后不再收到通知的路径。
        // TODO [L3][优化-策略模式] 当前 injectAll 硬编码按固定顺序调用 5 个私有注入方法。可抽象为 List<InjectionStrategy>（每个注解类型一个策略：ResourceFieldStrategy / ResourceMethodStrategy / AutowiredFieldStrategy / AutowiredMethodStrategy / ValueFieldStrategy），由排序器组合后遍历执行，消除硬编码顺序与未来扩展新注解时的修改点。；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
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

            // TODO [L2][练习] 手写 @Resource 字段的「先按 name 再按 type」回退解析——entry.resourceName 非空且容器 containsBean(name) 时按名称取 bean，否则回退到按字段类型 getBean(type)；写对标志：name 命中返回对应 bean，name 缺失/不命中时按类型正确返回。（具体解析见 resolveResourceByNameOrType）
            Object dependency;
            if (entry.lazyProxy != null) {
                // TODO [L1][练习] 实现 @Lazy 代理注入分支——当 entry.lazyProxy 不为 null 时直接注入该代理对象，跳过实时依赖解析；写对标志：注入后字段引用的是代理而非真实 bean，调用时才触发真实解析。
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

            // TODO [L2][练习] 手写 @Autowired 字段按类型解析——调用 container.resolveDependencyWithGenerics(field, qualifier, required) 拿到依赖；当 required=false 且返回 null 时静默跳过（不抛异常），required=true 且缺失则抛 DependencyResolutionException；写对标志：可选依赖缺失时 bean 字段保持 null 且不报错，必需依赖缺失时抛异常。
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

            // TODO [L1][练习] 手写 @Resource 方法（setter）注入——校验方法参数个数为 1，用 resolveResourceByNameOrType 取到依赖后 method.setAccessible(true); method.invoke(bean, dependency)；写对标志：setter 被调用一次且依赖成功注入。
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

            // TODO [L2][练习] 手写 @Autowired 方法注入——取参数类型 paramType，结合 entry.qualifier/required 调用 container.resolveDependency(paramType, methodName, qualifier, required)，非 null 时 setAccessible + invoke，required 缺失则抛错；写对标志：setter 被调用、依赖注入成功，可选依赖缺失不报错。
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

            // TODO [L1][练习] 手写 @Value 字段注入的「占位符解析 + 类型转换」衔接——先用 placeholderResolver.resolvePlaceholder(placeholder) 拿到字符串，再用 typeConverter.convert(resolvedValue, entry.type) 转为目标类型后注入；写对标志：@Value("${app.port}") 的 int 字段被正确赋值为整数。
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
    // TODO [L3][优化-责任链] 依赖解析目前只有「name→type」两级。可拆成责任链节点：LazyProxyNode → NameNode → TypeNode → QualifierNode → GenericNode，依次尝试直到命中，便于扩展（如增加 @Primary 优先、@Profile 过滤），且每个节点可独立测试。；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的主路径与一条全链放行的路径。
    private Object resolveResourceByNameOrType(String beanName, String name, Class<?> type) throws Exception {
        if (name != null && container.containsBean(name)) {
            return container.getBean(name, type);
        }
        return container.getBean(type);
    }
}
