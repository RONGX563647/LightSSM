package com.lightframework.ioc.core;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Lazy;
import com.lightframework.ioc.annotation.Qualifier;
import com.lightframework.ioc.annotation.Resource;
import com.lightframework.ioc.annotation.Value;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.ApplicationContext;
import com.lightframework.ioc.event.ApplicationEvent;
import com.lightframework.ioc.event.ApplicationEventPublisher;
import com.lightframework.ioc.exception.BeanCreationException;
import com.lightframework.ioc.exception.BeanCurrentlyInCreationException;
import com.lightframework.ioc.exception.NoSuchBeanDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Function;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.BitSet;
import java.util.ArrayDeque;
import java.util.Deque;

public class DefaultListableBeanFactory implements ListableBeanFactory, BeanDefinitionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactory.class);

    // FactoryBean 前缀，用于获取 FactoryBean 本身而非其创建的对象
    public static final String FACTORY_BEAN_PREFIX = "&";

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

    // FactoryBean 创建的对象缓存
    private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<>(64);


    private final List<BeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();

    // 别名映射
    private final Map<String, String> aliasMap = new ConcurrentHashMap<>(64);

    // 反射缓存：缓存每个类的 @Autowired 构造器
    private final Map<Class<?>, Constructor<?>> cachedAutowiredConstructors = new ConcurrentHashMap<>(256);

    // 反射缓存：缓存每个类的无参构造器
    private final Map<Class<?>, Constructor<?>> cachedDefaultConstructors = new ConcurrentHashMap<>(256);


    // 注解元数据缓存：合并所有注入注解（@Autowired, @Resource, @Value）的字段和方法收集（包括父类）
    // 使用 LRU 缓存避免无限增长（最多 512 个类）
    private final Map<Class<?>, AnnotationMetadata> cachedAnnotationMetadata = 
        Collections.synchronizedMap(new java.util.LinkedHashMap<Class<?>, AnnotationMetadata>(512, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Class<?>, AnnotationMetadata> eldest) {
                return size() > 512;
            }
        });

    // SPI: 缓存每个类的 setter 方法，用于 applyPropertyValues
    private final Map<Class<?>, Map<String, java.lang.reflect.Method>> cachedSetterMethods = new ConcurrentHashMap<>(64);

    // 类型索引缓存：缓存 getBeanNamesForType 的结果
    private final Map<Class<?>, String[]> cachedBeanNamesForType = new ConcurrentHashMap<>(256);

    // PropertyPlaceholderConfigurer 用于解析 @Value 占位符
    private PropertyPlaceholderConfigurer propertyPlaceholderConfigurer;

    // ApplicationContext 引用，用于 ApplicationContextAware 接口
    private ApplicationContext applicationContext;

    // Event publisher reference, for beans to publish events through BeanFactory
    private ApplicationEventPublisher eventPublisher;

    // #2: freeze flag — after preInstantiateSingletons, no new registrations allowed
    private volatile boolean frozen = false;

    // #8 (shortcut): fast singleton lookup once all singletons are created
    private volatile boolean singletonsCreated = false;

    // #7: frozen BPP array for zero-overhead iteration
    private volatile BeanPostProcessor[] frozenBpps = new BeanPostProcessor[0];

    // #3: 类型索引 — 在 registerBeanDefinition 时增量构建
    private final ConcurrentHashMap<Class<?>, List<String>> typeIndex = new ConcurrentHashMap<>(512);


    // Bean 创建时间统计（纳秒）
    private final Map<String, Long> beanCreationTimes = new ConcurrentHashMap<>(256);

    // Phase 2: dependency edges for dumpGraph()
    private final List<String[]> dependencyEdges = java.util.Collections.synchronizedList(new ArrayList<>());

    // Phase 4: Extracted components for SRP compliance
    private final SingletonCache singletonCache = new SingletonCache();
    private final FastBeanLookup fastBeanLookup = new FastBeanLookup();
    private final DefaultTypeConverter typeConverter = new DefaultTypeConverter();
    private final BeanLifecycleManager lifecycleManager = new BeanLifecycleManager();
    private InjectionEngine injectionEngine;

    public void setInjectionEngine(InjectionEngine injectionEngine) {
        this.injectionEngine = injectionEngine;
    }

    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if (beanName == null || beanDefinition == null) {
            throw new IllegalArgumentException("beanName and beanDefinition must not be null");
        }
        if (frozen) {
            throw new IllegalStateException("BeanFactory is frozen, cannot register new bean definition '" + beanName + "'");
        }
        this.beanDefinitionMap.put(beanName, beanDefinition);
        this.cachedBeanNamesForType.clear();
        // #3: 增量构建类型索引
        Class<?> beanClass = beanDefinition.getBeanClass();
        if (beanClass != null) {
            indexTypeRecursive(beanClass, beanName);
        }
        if (logger.isDebugEnabled()) logger.debug("Registered bean definition: {}", beanName);
    }

    private void indexTypeRecursive(Class<?> type, String beanName) {
        if (type == null || type == Object.class) return;
        indexType(type, beanName);
        for (Class<?> iface : type.getInterfaces()) {
            indexTypeRecursive(iface, beanName);
        }
        indexTypeRecursive(type.getSuperclass(), beanName);
    }

    private void indexType(Class<?> type, String beanName) {
        typeIndex.compute(type, (k, list) -> {
            if (list == null) list = new ArrayList<>(4);
            if (!list.contains(beanName)) list.add(beanName);
            return list;
        });
    }

    // 注册别名
    public void registerAlias(String beanName, String alias) {
        if (beanName == null) {
            throw new IllegalArgumentException("beanName must not be null");
        }
        if (alias == null || alias.isEmpty()) {
            return;
        }
        if (alias.equals(beanName)) {
            return;
        }
        if (this.beanDefinitionMap.containsKey(alias)) {
            throw new IllegalArgumentException("Alias '" + alias + "' is already in use for bean: " + alias);
        }
        this.aliasMap.put(alias, beanName);
        if (logger.isDebugEnabled()) logger.debug("Registered alias '{}' for bean '{}'", alias, beanName);
    }

    // 解析别名（支持链式别名，带循环检测）
    protected String resolveAlias(String name) {
        String resolved = name;
        Set<String> visited = new java.util.HashSet<>();
        int maxDepth = 100; // 防止无限循环
        int depth = 0;
        while (this.aliasMap.containsKey(resolved)) {
            if (!visited.add(resolved)) {
                throw new IllegalArgumentException("Circular alias detected: " + resolved);
            }
            if (++depth > maxDepth) {
                throw new IllegalArgumentException("Alias chain too deep, possible circular reference: " + name);
            }
            resolved = this.aliasMap.get(resolved);
        }
        return resolved;
    }

    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
        // 按 @Order 注解排序，确保 BPP 按正确顺序执行
        this.beanPostProcessors.sort((a, b) -> {
            int orderA = getOrderValue(a);
            int orderB = getOrderValue(b);
            return Integer.compare(orderA, orderB);
        });
    }
    
    private int getOrderValue(Object obj) {
        if (obj.getClass().isAnnotationPresent(com.lightframework.ioc.annotation.Order.class)) {
            return obj.getClass().getAnnotation(com.lightframework.ioc.annotation.Order.class).value();
        }
        // 检查 Ordered 接口
        if (obj instanceof com.lightframework.ioc.core.Ordered) {
            return ((com.lightframework.ioc.core.Ordered) obj).getOrder();
        }
        return 0; // 默认顺序
    }

    @Override
    public Object getBean(String name) throws Exception {
        // #8 (shortcut): after all singletons are created, bypass full bean lifecycle
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            return doGetBean(name.substring(1), null, true);
        }
        if (singletonsCreated) {
            String resolved = resolveSingleAlias(name);
            // Phase 2: fast table — array access instead of HashMap.get()
            Object singleton = fastLookup(resolved);
            if (singleton != null) {
                return singleton;
            }
        }
        return doGetBean(name, null, false);
    }

    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws Exception {
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            Object factoryBean = doGetBean(name.substring(1), null, true);
            return requiredType != null ? requiredType.cast(factoryBean) : (T) factoryBean;
        }
        if (singletonsCreated) {
            String resolved = resolveSingleAlias(name);
            // Phase 2: fast table
            Object singleton = fastLookup(resolved);
            if (singleton != null) {
                return requiredType != null ? requiredType.cast(singleton) : (T) singleton;
            }
        }
        Object bean = doGetBean(name, requiredType, false);
        return requiredType != null ? requiredType.cast(bean) : (T) bean;
    }

    // Phase 2: fast bean lookup — O(1) array access for hot beans (with hash collision check)
    private Object fastLookup(String name) {
        Object result = fastBeanLookup.lookup(name);
        return result != null ? result : singletonCache.getSingletonObjects().get(name);
    }

    // #6: resolve alias in a single hop (no loop, used in fast path)
    // 注意：只解析一级别名，链式别名由 resolveAlias 处理
    private String resolveSingleAlias(String name) {
        String resolved = this.aliasMap.get(name);
        return resolved != null ? resolved : name;
    }

    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        String[] beanNames = getBeanNamesForType(requiredType);
        if (beanNames.length == 0) {
            // Phase 3: smart error with available bean names for Levenshtein matching
            throw new NoSuchBeanDefinitionException(requiredType, 
                "No qualifying bean of type '" + requiredType.getName() + "' available\n" +
                buildTypeHint(requiredType));
        }
        if (beanNames.length == 1) {
            return getBean(beanNames[0], requiredType);
        }
        return getPrimaryBean(requiredType);
    }

    // 通过 qualifier 获取 Bean
    public <T> T getBeanByQualifier(Class<T> requiredType, String qualifier) throws Exception {
        String[] beanNames = getBeanNamesForType(requiredType);
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd != null && qualifier.equals(bd.getQualifier())) {
                return getBean(beanName, requiredType);
            }
        }
        // 如果 qualifier 匹配 beanName，也支持
        for (String beanName : beanNames) {
            if (qualifier.equals(beanName)) {
                return getBean(beanName, requiredType);
            }
        }
        throw new NoSuchBeanDefinitionException("No bean found for qualifier: " + qualifier + " and type: " + requiredType.getName());
    }

    @Override
    public boolean containsBean(String name) {
        String actualName = name.startsWith(FACTORY_BEAN_PREFIX) ? name.substring(1) : name;
        String resolved = resolveAlias(actualName);
        return singletonCache.isBeanCreated(resolved) || this.beanDefinitionMap.containsKey(resolved);
    }

    @Override
    public boolean isSingleton(String name) {
        // 处理 FactoryBean 前缀
        String actualName = name.startsWith(FACTORY_BEAN_PREFIX) ? name.substring(1) : name;
        BeanDefinition bd = this.beanDefinitionMap.get(resolveAlias(actualName));
        return bd != null && bd.isSingleton();
    }

    @Override
    public boolean isPrototype(String name) {
        // 处理 FactoryBean 前缀
        String actualName = name.startsWith(FACTORY_BEAN_PREFIX) ? name.substring(1) : name;
        BeanDefinition bd = this.beanDefinitionMap.get(resolveAlias(actualName));
        return bd != null && bd.isPrototype();
    }

    @Override
    public Class<?> getType(String name) {
        String beanName = resolveAlias(name);
        
        // 如果是 FactoryBean 本身（以 "&" 开头）
        if (name.startsWith(FACTORY_BEAN_PREFIX)) {
            beanName = name.substring(1);
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            return bd != null ? bd.getBeanClass() : null;
        }
        
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd != null) {
            // 如果 Bean 是 FactoryBean 类型，返回其创建的对象类型
            if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                Object cachedObject = this.factoryBeanObjectCache.get(beanName);
                if (cachedObject != null) {
                    return cachedObject.getClass();
                }
                Object sharedInstance = getSingleton(beanName);
                if (sharedInstance instanceof FactoryBean) {
                    Class<?> objectType = ((FactoryBean<?>) sharedInstance).getObjectType();
                    if (objectType != null) {
                        return objectType;
                    }
                }
                // 未创建时临时实例化 FactoryBean 仅用于 getObjectType，避免完整 Bean 生命周期
                try {
                    FactoryBean<?> fb = (FactoryBean<?>) bd.getBeanClass().getDeclaredConstructor().newInstance();
                    Class<?> objectType = fb.getObjectType();
                    return objectType != null ? objectType : bd.getBeanClass();
                } catch (Exception e) {
                    return bd.getBeanClass();
                }
            }
            return bd.getBeanClass();
        }
        return null;
    }

    @Override
    public String[] getAliases(String name) {
        String resolved = resolveAlias(name);
        return this.aliasMap.entrySet().stream()
            .filter(e -> e.getValue().equals(resolved))
            .map(Map.Entry::getKey)
            .toArray(String[]::new);
    }

    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanDefinitionMap.containsKey(beanName);
    }

    @Override
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }

    @Override
    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[0]);
    }

    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        // #3: 先查 typeIndex（覆盖直接类型、接口、父类）
        List<String> indexed = typeIndex.get(type);
        if (indexed != null) {
            // 仍需要检查 FactoryBean 类型（其对象类型在注册时未知）
            List<String> result = new ArrayList<>(indexed);
            for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
                String beanName = entry.getKey();
                BeanDefinition bd = entry.getValue();
                if (!indexed.contains(beanName) && FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                    try {
                        Class<?> objectType = getType(beanName);
                        if (objectType != null && type.isAssignableFrom(objectType)) {
                            result.add(beanName);
                        }
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) logger.debug("Failed to get FactoryBean type for bean: {}", beanName);
                    }
                }
            }
            return result.toArray(new String[0]);
        }
        // 回退到全量遍历（例如请求的是 FactoryBean 产生的对象类型）
        return cachedBeanNamesForType.computeIfAbsent(type, t -> {
            List<String> result = new ArrayList<>();
            for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
                String beanName = entry.getKey();
                BeanDefinition bd = entry.getValue();
                if (t.isAssignableFrom(bd.getBeanClass())) {
                    result.add(beanName);
                } else if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                    try {
                        Class<?> objectType = getType(beanName);
                        if (objectType != null && t.isAssignableFrom(objectType)) {
                            result.add(beanName);
                        }
                    } catch (Exception e) {
                        if (logger.isDebugEnabled()) logger.debug("Failed to get FactoryBean type for bean: {}", beanName);
                    }
                }
            }
            return result.toArray(new String[0]);
        });
    }

    @Override
    public <T> List<T> getBeansOfType(Class<T> type) throws Exception {
        return new ArrayList<>(getBeansOfTypeAsMap(type).values());
    }

    @Override
    public <T> Map<String, T> getBeansOfTypeAsMap(Class<T> type) throws Exception {
        Map<String, T> result = new LinkedHashMap<>();
        String[] beanNames = getBeanNamesForType(type);
        List<String> failedBeans = new ArrayList<>();
        
        for (String beanName : beanNames) {
            if (singletonCache.isSingletonCurrentlyInCreation(beanName)) {
                continue;
            }
            try {
                result.put(beanName, getBean(beanName, type));
            } catch (Exception e) {
                failedBeans.add(beanName);
                if (logger.isDebugEnabled()) logger.debug("Failed to get bean '{}' of type {}: {}", beanName, type.getName(), e.getMessage());
            }
        }
        
        if (!failedBeans.isEmpty() && logger.isWarnEnabled()) {
            logger.warn("Failed to get {} beans of type {}: {}", failedBeans.size(), type.getName(), failedBeans);
        }
        
        return result;
    }

    @Override
    public <T> T getPrimaryBean(Class<T> type) throws Exception {
        String[] beanNames = getBeanNamesForType(type);
        String primaryBeanName = null;
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd != null && bd.isPrimary()) {
                if (primaryBeanName != null) {
                    throw new NoSuchBeanDefinitionException(type, 
                        "No unique bean of type '" + type.getName() + "' is defined: " +
                        "expected single matching bean but found 2: " + primaryBeanName + "," + beanName);
                }
                primaryBeanName = beanName;
            }
        }
        if (primaryBeanName != null) {
            return getBean(primaryBeanName, type);
        }
        throw new NoSuchBeanDefinitionException("No primary bean found for type: " + type.getName());
    }
    
    protected <T> T doGetBean(String name, Class<T> requiredType, boolean returnFactoryBean) throws Exception {
        String beanName = resolveAlias(name);
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd == null) {
            throw new NoSuchBeanDefinitionException(name, this.beanDefinitionMap.keySet().toArray(new String[0]));
        }

        // 如果 returnFactoryBean 为 true，直接返回 FactoryBean 本身（不经过 FactoryBean 逻辑）
        if (returnFactoryBean) {
            Object instance;
            if (bd.isSingleton()) {
                instance = getSingleton(beanName);
                if (instance == null) {
                    instance = createSingletonBean(beanName, bd);
                }
            } else {
                instance = createBean(beanName, bd);
            }
            return requiredType != null ? requiredType.cast(instance) : (T) instance;
        }

        if (bd.isSingleton()) {
            Object sharedInstance = getSingleton(beanName);
            if (sharedInstance == null) {
                sharedInstance = createSingletonBean(beanName, bd);
            }
            // 性能优化：先检查 BeanDefinition 的 beanClass，避免 instanceof 失败时的开销
            if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                return getFactoryBeanObjectFromInstance(beanName, sharedInstance, requiredType);
            }
            return requiredType != null ? requiredType.cast(sharedInstance) : (T) sharedInstance;
        } else {
            Object beanInstance = createBean(beanName, bd);
            // 性能优化：先检查 BeanDefinition 的 beanClass
            if (FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
                return getFactoryBeanObjectFromInstance(beanName, beanInstance, requiredType);
            }
            return requiredType != null ? requiredType.cast(beanInstance) : (T) beanInstance;
        }
    }
    
    /**
     * 从已实例化的 FactoryBean 获取其创建的对象
     */
    @SuppressWarnings("unchecked")
    private <T> T getFactoryBeanObjectFromInstance(String beanName, Object instance, Class<T> requiredType) throws Exception {
        if (instance instanceof FactoryBean) {
            FactoryBean<?> factory = (FactoryBean<?>) instance;
            return getFactoryBeanObject(beanName, factory, requiredType);
        }
        return requiredType != null ? requiredType.cast(instance) : (T) instance;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getFactoryBeanObject(String beanName, FactoryBean<?> factory, Class<T> requiredType) throws Exception {
        Object cachedObject;
        if (factory.isSingleton()) {
            // 单例模式：从缓存中获取，如果不存在则创建并缓存
            cachedObject = this.factoryBeanObjectCache.get(beanName);
            if (cachedObject == null) {
                cachedObject = factory.getObject();
                if (cachedObject != null) {
                    // 对 FactoryBean 创建的对象也应用生命周期回调
                    cachedObject = initializeBean(beanName + "_$factoryBean", cachedObject, 
                            new BeanDefinition(beanName, cachedObject.getClass()));
                    this.factoryBeanObjectCache.put(beanName, cachedObject);
                }
            }
        } else {
            // 原型模式：每次创建新对象
            cachedObject = factory.getObject();
            if (cachedObject != null) {
                cachedObject = initializeBean(beanName + "_$factoryBean", cachedObject,
                        new BeanDefinition(beanName, cachedObject.getClass()));
            }
        }
        return requiredType != null ? requiredType.cast(cachedObject) : (T) cachedObject;
    }

    protected Object getSingleton(String beanName) {
        return singletonCache.getSingleton(beanName);
    }

    protected Object createSingletonBean(String beanName, BeanDefinition bd) throws Exception {
        Object bean = doCreateBean(beanName, bd);
        addSingleton(beanName, bean);
        return bean;
    }

    protected Object createBean(String beanName, BeanDefinition bd) throws Exception {
        return doCreateBean(beanName, bd);
    }

    protected Object doCreateBean(String beanName, BeanDefinition bd) throws Exception {
        long start = System.nanoTime();
        try {
            // 检查并初始化 dependsOn 声明的依赖
            String[] dependsOn = bd.getDependsOn();
            if (dependsOn != null && dependsOn.length > 0) {
                for (String dependencyName : dependsOn) {
                    getBean(dependencyName);
                    // Phase 3: record edge for dumpGraph
                    recordDependencyEdge(beanName, dependencyName);
                    if (logger.isDebugEnabled()) logger.debug("Bean '{}' depends on '{}', ensured dependency is initialized", beanName, dependencyName);
                }
            }

            beforeSingletonCreation(beanName);

            // 1. 实例化 Bean（创建原始对象）
            Object beanInstance = instantiateBean(beanName, bd);

            // ★ 应用 BeanDefinition 中的属性值 (SPI: MyBatis MapperFactoryBean 等)
            applyPropertyValues(beanName, beanInstance, bd);

            // 2. 对于单例 Bean，立即提前暴露引用（在依赖注入之前），用于解决循环依赖
            boolean earlySingletonExposure = bd.isSingleton();
            if (earlySingletonExposure) {
                addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, bd, beanInstance));
                if (logger.isDebugEnabled()) logger.debug("Exposed early singleton reference for bean: {}", beanName);
            }

            // 3. 依赖注入（此时如果存在循环依赖，可以从二级缓存获取早期引用）
            populateBean(beanName, bd, beanInstance);

            // 4. 初始化 Bean（调用 @PostConstruct 和 BeanPostProcessor）
            Object exposedObject = initializeBean(beanName, beanInstance, bd);

            // 5. 处理早期引用：如果 Bean 被提前暴露过，返回早期引用（保证循环依赖中的对象是同一个）
            if (earlySingletonExposure) {
                Object earlySingletonReference = getSingleton(beanName);
                if (earlySingletonReference != null && earlySingletonReference != beanInstance) {
                    exposedObject = earlySingletonReference;
                }
            }

            return exposedObject;
        } finally {
            beanCreationTimes.put(beanName, System.nanoTime() - start);
            afterSingletonCreation(beanName);
        }
    }

    protected Object instantiateBean(String beanName, BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        try {
            // 先从缓存获取构造器
            Constructor<?> ctor = cachedAutowiredConstructors.get(beanClass);
            if (ctor == null) {
                // 双重检查
                synchronized (cachedAutowiredConstructors) {
                    ctor = cachedAutowiredConstructors.get(beanClass);
                    if (ctor == null) {
                        // 外部获取，不在 computeIfAbsent lambda 中
                        Constructor<?> foundAutowired = null;
                        Constructor<?> foundDefault = null;
                        int autowiredCount = 0;
                        for (Constructor<?> c : beanClass.getDeclaredConstructors()) {
                            if (c.isAnnotationPresent(Autowired.class)) {
                                foundAutowired = c;
                                autowiredCount++;
                            }
                            if (c.getParameterCount() == 0) {
                                foundDefault = c;
                            }
                        }
                        if (autowiredCount > 1) {
                            throw new BeanCreationException(beanClass.getName(),
                                "Multiple @Autowired constructors found: " + autowiredCount +
                                ". Only one @Autowired constructor is allowed.");
                        }
                        // 缓存无参构造器（在 lambda 外部调用，安全）
                        if (foundDefault != null) {
                            cachedDefaultConstructors.put(beanClass, foundDefault);
                        }
                        // 缓存 @Autowired 构造器（仅当存在时，因为 ConcurrentHashMap 不接受 null）
                        if (foundAutowired != null) {
                            cachedAutowiredConstructors.put(beanClass, foundAutowired);
                        }
                        ctor = foundAutowired;
                    }
                }
            }

            if (ctor != null) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    String qualifier = null;
                    boolean isLazy = false;
                    java.lang.annotation.Annotation[] paramAnnotations = ctor.getParameterAnnotations()[i];
                    for (java.lang.annotation.Annotation ann : paramAnnotations) {
                        if (ann instanceof Qualifier) {
                            qualifier = ((Qualifier) ann).value();
                        } else if (ann instanceof Lazy) {
                            isLazy = ((Lazy) ann).value();
                        }
                    }
                    // 支持构造器参数 @Lazy
                    if (isLazy) {
                        args[i] = createLazyProxy(paramTypes[i], qualifier);
                        if (args[i] == null) {
                            args[i] = resolveDependency(paramTypes[i], null, qualifier, true);
                        }
                    } else {
                        args[i] = resolveDependency(paramTypes[i], null, qualifier, true);
                    }
                }
                ctor.setAccessible(true);
                Object instance = ctor.newInstance(args);
                if (logger.isDebugEnabled()) logger.debug("Instantiated bean with @Autowired constructor: {}", beanName);
                return instance;
            }

            // 获取无参构造器（同样使用 get + put 而非 computeIfAbsent）
            Constructor<?> constructor = cachedDefaultConstructors.get(beanClass);
            if (constructor == null) {
                synchronized (cachedDefaultConstructors) {
                    constructor = cachedDefaultConstructors.get(beanClass);
                    if (constructor == null) {
                        try {
                            constructor = beanClass.getDeclaredConstructor();
                            cachedDefaultConstructors.put(beanClass, constructor);
                        } catch (NoSuchMethodException e) {
                            throw new BeanCreationException(beanName, "No default constructor found for class: " + beanClass.getName());
                        }
                    }
                }
            }

            constructor.setAccessible(true);
            Object instance = constructor.newInstance();
            if (logger.isDebugEnabled()) logger.debug("Instantiated bean: {}", beanName);
            return instance;
        } catch (Exception e) {
            if (e instanceof BeanCreationException) {
                throw e;
            }
            throw new BeanCreationException(beanName, "Failed to instantiate bean", e);
        }
    }

    /**
     * ★ SPI: 应用 BeanDefinition 中的属性值到 bean 实例
     */
    protected void applyPropertyValues(String beanName, Object beanInstance, BeanDefinition bd) throws Exception {
        Map<String, Object> propertyValues = bd.getPropertyValues();
        if (propertyValues.isEmpty()) {
            return;
        }
        Class<?> beanClass = bd.getBeanClass();
        Map<String, java.lang.reflect.Method> setterCache = cachedSetterMethods.computeIfAbsent(beanClass, clazz -> {
            Map<String, java.lang.reflect.Method> cache = new HashMap<>(8);
            for (java.lang.reflect.Method method : clazz.getMethods()) {
                if (method.getParameterCount() == 1 && method.getName().startsWith("set")) {
                    String propName = method.getName().substring(3);
                    if (propName.length() > 0) {
                        String key = Character.toLowerCase(propName.charAt(0)) + propName.substring(1);
                        cache.putIfAbsent(key, method);
                    }
                }
            }
            return cache;
        });
        for (Map.Entry<String, Object> entry : propertyValues.entrySet()) {
            String propName = entry.getKey();
            Object propValue = entry.getValue();
            java.lang.reflect.Method setter = setterCache.get(propName);
            if (setter != null) {
                try {
                    setter.invoke(beanInstance, propValue);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Set property '{}' = '{}' on bean '{}'", propName, propValue, beanName);
                    }
                } catch (Exception e) {
                    throw new BeanCreationException(beanName, "Failed to set property '" + propName + "'", e);
                }
            } else {
                logger.warn("No setter found for property '{}' on bean '{}'", propName, beanName);
            }
        }
    }

    public void populateBean(String beanName, BeanDefinition bd, Object bean) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        if (beanClass == null) {
            throw new BeanCreationException(beanName, "Bean class is null, cannot populate bean");
        }

        AnnotationMetadata metadata = cachedAnnotationMetadata.get(beanClass);
        if (metadata == null) {
            metadata = cachedAnnotationMetadata.computeIfAbsent(beanClass, clazz -> {
                AnnotationMetadata m = new AnnotationMetadata();
                resolveAnnotationMetadata(clazz, m);
                m.buildFieldIndex();
                return m;
            });
        }

        Map<Object, Integer> fieldIdx = metadata.fieldIndex;
        BitSet injectedFieldBits = new BitSet(64);
        BitSet injectedMethodBits = new BitSet(64);

        // 使用统一的注入引擎处理所有注解注入
        if (injectionEngine != null) {
            injectionEngine.injectAll(beanName, bean, metadata, fieldIdx, injectedFieldBits, injectedMethodBits);
        } else {
            // 回退到内联注入逻辑（单元测试场景）
            injectResourceFields(beanName, bean, metadata, fieldIdx, injectedFieldBits);
            injectResourceMethods(beanName, bean, metadata, fieldIdx, injectedMethodBits);
            injectAutowiredFields(beanName, bean, metadata, fieldIdx, injectedFieldBits);
            injectAutowiredMethods(beanName, bean, metadata, fieldIdx, injectedMethodBits);
            if (propertyPlaceholderConfigurer != null) {
                injectValueFields(beanName, bean, metadata);
            }
        }
    }

    private void injectResourceFields(String beanName, Object bean, AnnotationMetadata metadata,
                                        java.util.Map<Object, Integer> fieldIdx, BitSet injectedFieldBits) throws Exception {
        for (int i = 0; i < metadata.resourceFields.size(); i++) {
            Field field = metadata.resourceFields.get(i);
            AnnotationInjectEntry entry = i < metadata.resourceEntries.size() ? metadata.resourceEntries.get(i) : null;
            Resource resource = field.getAnnotation(Resource.class);
            if (resource == null) continue;

            Lazy lazy = field.getAnnotation(Lazy.class);
            if (lazy != null && lazy.value()) {
                String name = resource.name().isEmpty() ? field.getName() : resource.name();
                Class<?> type = resource.type() == Object.class ? field.getType() : resource.type();
                Object lazyProxy = createLazyProxy(type, name);
                if (lazyProxy != null) {
                    if (entry != null) entry.injector.inject(bean, lazyProxy);
                    else { field.setAccessible(true); field.set(bean, lazyProxy); }
                    Integer bit = fieldIdx.get(field);
                    if (bit != null) injectedFieldBits.set(bit);
                    if (logger.isDebugEnabled()) logger.debug("@Lazy @Resource injected field {} in bean {}", field.getName(), beanName);
                    continue;
                }
            }

            String name = resource.name().isEmpty() ? field.getName() : resource.name();
            Class<?> type = resource.type() == Object.class ? field.getType() : resource.type();
            Object dependency;
            try {
                if (containsBean(name)) {
                    dependency = getBean(name, type);
                } else {
                    dependency = getBean(type);
                }
            } catch (NoSuchBeanDefinitionException e) {
                throw new BeanCreationException(beanName,
                    "Required dependency not found for @Resource field: " + field.getName() +
                    " (name='" + name + "', type=" + type.getSimpleName() + ")", e);
            }

            if (dependency != null) {
                if (entry != null) entry.injector.inject(bean, dependency);
                else { field.setAccessible(true); field.set(bean, dependency); }
                Integer bit = fieldIdx.get(field);
                if (bit != null) injectedFieldBits.set(bit);
                if (logger.isDebugEnabled()) logger.debug("@Resource injected field {} in bean {}", field.getName(), beanName);
            } else {
                throw new BeanCreationException(beanName, "Required dependency not found for @Resource field: " + field.getName());
            }
        }
    }

    private void injectResourceMethods(String beanName, Object bean, AnnotationMetadata metadata,
                                        java.util.Map<Object, Integer> fieldIdx, BitSet injectedMethodBits) throws Exception {
        for (Method method : metadata.resourceMethods) {
            Resource resource = method.getAnnotation(Resource.class);
            if (resource == null || method.getParameterCount() != 1) continue;

            String name = resource.name();
            if (name.isEmpty()) {
                name = decapitalize(method.getName().startsWith("set") ? method.getName().substring(3) : method.getName());
            }
            Class<?> type = resource.type() == Object.class ? method.getParameterTypes()[0] : resource.type();
            Object dependency;
            try {
                if (containsBean(name)) {
                    dependency = getBean(name, type);
                } else {
                    dependency = getBean(type);
                }
            } catch (NoSuchBeanDefinitionException e) {
                throw new BeanCreationException(beanName,
                    "Required dependency not found for @Resource method: " + method.getName() +
                    " (name='" + name + "', type=" + type.getSimpleName() + ")", e);
            }

            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                Integer bit = fieldIdx.get(method);
                if (bit != null) injectedMethodBits.set(bit);
                if (logger.isDebugEnabled()) logger.debug("@Resource injected method {} in bean {}", method.getName(), beanName);
            } else {
                throw new BeanCreationException(beanName, "Required dependency not found for @Resource method: " + method.getName());
            }
        }
    }

    private void injectAutowiredFields(String beanName, Object bean, AnnotationMetadata metadata,
                                        java.util.Map<Object, Integer> fieldIdx, BitSet injectedFieldBits) throws Exception {
        for (int i = 0; i < metadata.autowiredFields.size(); i++) {
            Field field = metadata.autowiredFields.get(i);
            AnnotationInjectEntry entry = i < metadata.autowiredEntries.size() ? metadata.autowiredEntries.get(i) : null;
            Integer bit = fieldIdx.get(field);
            if (bit != null && injectedFieldBits.get(bit)) continue;

            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired == null) continue;

            Lazy lazy = field.getAnnotation(Lazy.class);
            if (lazy != null && lazy.value()) {
                String qualifier = extractQualifier(field);
                Object lazyProxy = createLazyProxy(field.getType(), qualifier);
                if (lazyProxy != null) {
                    if (entry != null) entry.injector.inject(bean, lazyProxy);
                    else { field.setAccessible(true); field.set(bean, lazyProxy); }
                    if (bit != null) injectedFieldBits.set(bit);
                    if (logger.isDebugEnabled()) logger.debug("@Lazy @Autowired field {} in bean {}", field.getName(), beanName);
                    continue;
                }
            }

            String qualifier = extractQualifier(field);
            Object dependency = resolveDependencyWithGenerics(field, qualifier, autowired.required());
            if (dependency != null) {
                if (entry != null) entry.injector.inject(bean, dependency);
                else { field.setAccessible(true); field.set(bean, dependency); }
                if (bit != null) injectedFieldBits.set(bit);
                if (logger.isDebugEnabled()) logger.debug("Autowired field {} in bean {}", field.getName(), beanName);
            } else if (autowired.required()) {
                throw new BeanCreationException(beanName, "Required dependency not found for field: " + field.getName());
            }
        }
    }

    private void injectAutowiredMethods(String beanName, Object bean, AnnotationMetadata metadata,
                                         java.util.Map<Object, Integer> fieldIdx, BitSet injectedMethodBits) throws Exception {
        for (Method method : metadata.autowiredMethods) {
            Integer bit = fieldIdx.get(method);
            if (bit != null && injectedMethodBits.get(bit)) continue;

            Autowired autowired = method.getAnnotation(Autowired.class);
            if (autowired == null || method.getParameterCount() != 1) continue;

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
            Object dependency = resolveDependency(paramType, method.getName(), qualifier, autowired.required());
            if (dependency != null) {
                method.setAccessible(true);
                method.invoke(bean, dependency);
                if (bit != null) injectedMethodBits.set(bit);
                if (logger.isDebugEnabled()) logger.debug("Autowired setter {} in bean {}", method.getName(), beanName);
            } else if (autowired.required()) {
                throw new BeanCreationException(beanName, "Required dependency not found for setter: " + method.getName());
            }
        }
    }

    // Phase 3: build type hint for smart error messages - use Levenshtein distance to suggest similar types
    private String buildTypeHint(Class<?> type) {
        String targetName = type.getSimpleName().toLowerCase();
        List<Map.Entry<Class<?>, Integer>> scored = new ArrayList<>();
        
        for (Map.Entry<Class<?>, List<String>> e : this.typeIndex.entrySet()) {
            String candidateName = e.getKey().getSimpleName().toLowerCase();
            int distance = levenshteinDistance(targetName, candidateName);
            scored.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), distance));
        }
        
        scored.sort(java.util.Map.Entry.comparingByValue());
        
        StringBuilder sb = new StringBuilder();
        sb.append("  Did you mean one of these?\n");
        int count = 0;
        for (Map.Entry<Class<?>, Integer> entry : scored) {
            if (count >= 5) break;
            sb.append("    - ").append(entry.getKey().getSimpleName());
            sb.append(" (similarity: ").append(100 - entry.getValue()).append("%)\n");
            count++;
        }
        return sb.toString();
    }

    // 计算两个字符串的 Levenshtein 距离（优化：使用 1D 数组减少内存分配）
    private int levenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }
        int m = s1.length();
        int n = s2.length();
        if (m == 0) return n;
        if (n == 0) return m;
        
        int[] prev = new int[n + 1];
        int[] curr = new int[n + 1];
        for (int j = 0; j <= n; j++) prev[j] = j;
        
        for (int i = 1; i <= m; i++) {
            curr[0] = i;
            for (int j = 1; j <= n; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(prev[j] + 1, curr[j - 1] + 1), prev[j - 1] + cost);
            }
            // swap arrays
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }
        return prev[n];
    }

    // 将首字母小写
    private String decapitalize(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) && Character.isUpperCase(name.charAt(0))) {
            return name;
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    // 创建懒加载代理对象（仅支持接口类型，使用 JDK 动态代理）
    public Object createLazyProxy(Class<?> type, String beanName) {
        if (type.isInterface()) {
            return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type},
                    new LazyInvocationHandler(type, beanName)
            );
        }
        net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
        enhancer.setSuperclass(type);
        enhancer.setCallback(new net.sf.cglib.proxy.MethodInterceptor() {
            private volatile Object target;
            @Override
            public Object intercept(Object obj, Method method, Object[] args,
                                   net.sf.cglib.proxy.MethodProxy proxy) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    String mn = method.getName();
                    if (mn.equals("toString") && method.getParameterCount() == 0) {
                        return "Lazy CGLIB proxy for " + type.getName() + " [not initialized]";
                    }
                    return proxy.invokeSuper(obj, args);
                }
                if (target == null) {
                    synchronized (this) {
                        if (target == null) {
                            target = getBean(beanName, type);
                        }
                    }
                }
                return method.invoke(target, args);
            }
        });
        return enhancer.create();
    }

    // 懒加载代理的 InvocationHandler
    private class LazyInvocationHandler implements InvocationHandler {
        private final Class<?> type;
        private final String beanName;
        private volatile Object target;

        public LazyInvocationHandler(Class<?> type, String beanName) {
            this.type = type;
            this.beanName = beanName;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 特殊处理 Object 方法，不需要初始化 target
            String methodName = method.getName();
            if (methodName.equals("toString") && method.getParameterCount() == 0) {
                return "Lazy proxy for " + type.getName() + (target != null ? " [initialized]" : " [not initialized]");
            }
            if (methodName.equals("hashCode") && method.getParameterCount() == 0) {
                return System.identityHashCode(proxy);
            }
            if (methodName.equals("equals") && method.getParameterCount() == 1) {
                return proxy == args[0];
            }
            
            // 双重检查锁定初始化 target
            if (target == null) {
                synchronized (this) {
                    if (target == null) {
                        if (beanName != null && !beanName.isEmpty()) {
                            try {
                                target = getBean(beanName, type);
                            } catch (Exception e) {
                                target = getBean(type);
                            }
                        } else {
                            target = getBean(type);
                        }
                        if (logger.isDebugEnabled()) logger.debug("Lazy proxy initialized bean of type {} (name: {})", type.getName(), beanName);
                    }
                }
            }
            // 只在必要时调用 setAccessible（接口方法默认 public，不需要）
            if (!method.canAccess(target)) {
                method.setAccessible(true);
            }
            return method.invoke(target, args);
        }
    }

    // 创建 FieldInjector，优先使用 MethodHandle 加速
    private static FieldInjector createFieldInjector(Field field) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(
                    field.getDeclaringClass(), MethodHandles.lookup());
            MethodHandle setter = lookup.unreflectSetter(field);
            return (bean, value) -> {
                try {
                    setter.invoke(bean, value);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            };
        } catch (Exception e) {
            // Fall back to reflection
            field.setAccessible(true);
            return (bean, value) -> field.set(bean, value);
        }
    }

    // 单趟递归收集类层次结构中所有注入注解元数据
    private void resolveAnnotationMetadata(Class<?> clazz, AnnotationMetadata metadata) {
        if (clazz == null || clazz == Object.class) {
            return;
        }
        resolveAnnotationMetadata(clazz.getSuperclass(), metadata);

        // Aware 类型位掩码检测
        if (BeanNameAware.class.isAssignableFrom(clazz)) metadata.awareFlags |= 0x01;
        if (BeanFactoryAware.class.isAssignableFrom(clazz)) metadata.awareFlags |= 0x02;
        if (ApplicationContextAware.class.isAssignableFrom(clazz)) metadata.awareFlags |= 0x04;

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                metadata.autowiredFields.add(field);
                metadata.autowiredEntries.add(new AnnotationInjectEntry(field.getType(), createFieldInjector(field)));
            }
            if (field.isAnnotationPresent(Resource.class)) {
                metadata.resourceFields.add(field);
                metadata.resourceEntries.add(new AnnotationInjectEntry(field.getType(), createFieldInjector(field)));
            }
            if (field.isAnnotationPresent(Value.class)) {
                metadata.valueFields.add(field);
                metadata.valueEntries.add(new AnnotationInjectEntry(field.getType(), createFieldInjector(field)));
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge()) {
                continue;
            }
            if (method.isAnnotationPresent(Autowired.class)) {
                metadata.autowiredMethods.add(method);
            }
            if (method.isAnnotationPresent(Resource.class)) {
                metadata.resourceMethods.add(method);
            }
        }
    }

    // 注入 @Value 注解的字段（#1: 使用 FieldInjector 加速）
    private void injectValueFields(String beanName, Object bean, AnnotationMetadata metadata) throws Exception {
        for (int i = 0; i < metadata.valueFields.size(); i++) {
            Field field = metadata.valueFields.get(i);
            AnnotationInjectEntry entry = i < metadata.valueEntries.size() ? metadata.valueEntries.get(i) : null;
            Value valueAnnotation = field.getAnnotation(Value.class);
            if (valueAnnotation != null) {
                String placeholder = valueAnnotation.value();
                if (placeholder.isEmpty()) {
                    throw new BeanCreationException(beanName,
                            "@Value annotation on field '" + field.getName() + "' has an empty value");
                }

                // 解析占位符
                String resolvedValue = propertyPlaceholderConfigurer.resolvePlaceholder(placeholder);

                // 类型转换
                Object convertedValue;
                try {
                    convertedValue = convertValueIfNecessary(resolvedValue, field.getType());
                } catch (Exception e) {
                    throw new BeanCreationException(beanName, "Failed to convert @Value '" + placeholder + "' for field " + field.getName(), e);
                }

                if (entry != null) {
                    entry.injector.inject(bean, convertedValue);
                } else {
                    field.setAccessible(true);
                    field.set(bean, convertedValue);
                }
                if (logger.isDebugEnabled()) logger.debug("Injected @Value field {} with value '{}' in bean {}", field.getName(), resolvedValue, beanName);
            }
        }
    }

    private Object convertValueIfNecessary(String value, Class<?> targetType) throws Exception {
        return typeConverter.convert(value, targetType);
    }

    public Object initializeBean(String beanName, Object bean, BeanDefinition bd) throws Exception {
        // 1. 执行 BeanPostProcessor 前置处理
        Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);

        // 2. 调用 Aware 接口方法（在 @PostConstruct 之前）
        invokeAwareMethods(beanName, wrappedBean);

        // 3. 调用 @PostConstruct 和 InitializingBean
        invokeInitMethods(beanName, wrappedBean, bd);

        // 4. 执行 BeanPostProcessor 后置处理
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);

        return wrappedBean;
    }

    protected void invokeAwareMethods(String beanName, Object bean) throws Exception {
        // Phase 2: use precomputed awareFlags from AnnotationMetadata — no instanceof chain
        AnnotationMetadata meta = cachedAnnotationMetadata.get(bean.getClass());
        byte flags = meta != null ? meta.awareFlags : 0;
        if (flags != 0) {
            if ((flags & 0x01) != 0) ((BeanNameAware) bean).setBeanName(beanName);
            if ((flags & 0x02) != 0) ((BeanFactoryAware) bean).setBeanFactory(this);
            if ((flags & 0x04) != 0 && applicationContext != null) ((ApplicationContextAware) bean).setApplicationContext(applicationContext);
        }
    }

    public Object resolveDependency(Class<?> type, String fieldName, boolean required) throws Exception {
        return resolveDependency(type, fieldName, null, required);
    }

    public Object resolveDependency(Class<?> type, String fieldName, String qualifier, boolean required) throws Exception {
        try {
            // 如果指定了 qualifier，优先按 qualifier 查找
            if (qualifier != null && !qualifier.isEmpty()) {
                try {
                    return getBeanByQualifier(type, qualifier);
                } catch (NoSuchBeanDefinitionException e) {
                    // qualifier 匹配失败，回退到按类型查找
                    logger.debug("Qualifier '{}' not found for type {}, falling back to type matching", qualifier, type.getSimpleName());
                }
            }
            return getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            if (!required) {
                logger.debug("Optional dependency not found: {}#{}", type.getSimpleName(), fieldName);
                return null;
            }
            throw e;
        }
    }

    // 支持泛型类型解析的依赖解析
    protected Object resolveDependencyWithGenerics(Field field, String qualifier, boolean required) throws Exception {
        Type genericType = field.getGenericType();
        
        // 处理泛型类型，如 List<UserService>
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) genericType;
            Type rawType = pType.getRawType();
            Type[] actualTypes = pType.getActualTypeArguments();
            
            // 处理集合类型
            if (rawType == List.class || rawType == java.util.Collection.class) {
                if (actualTypes.length > 0 && actualTypes[0] instanceof Class) {
                    Class<?> elementType = (Class<?>) actualTypes[0];
                    try {
                        return getBeansOfType(elementType);
                    } catch (Exception e) {
                        if (!required) {
                            return new ArrayList<>();
                        }
                        throw e;
                    }
                }
            }
            
            // 处理 Map<String, T> 类型 — 注入所有该类型的 Bean，key 为 beanName
            if (rawType == Map.class && actualTypes.length >= 2) {
                if (actualTypes[0] == String.class && actualTypes[1] instanceof Class) {
                    Class<?> valueType = (Class<?>) actualTypes[1];
                    try {
                        return getBeansOfTypeAsMap(valueType);
                    } catch (Exception e) {
                        if (!required) {
                            return new LinkedHashMap<>();
                        }
                        throw e;
                    }
                }
            }
        }
        
        // 非泛型类型，使用普通解析
        return resolveDependency(field.getType(), field.getName(), qualifier, required);
    }

    // 从注解中提取 qualifier 值
    protected String extractQualifier(java.lang.reflect.AnnotatedElement element) {
        Qualifier qualifier = element.getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) {
            return qualifier.value();
        }
        return null;
    }

    protected Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
        Object exposedObject = bean;
        // #7: 使用冻结数组
        BeanPostProcessor[] bpps = this.frozenBpps.length > 0 ? this.frozenBpps
                : this.beanPostProcessors.toArray(new BeanPostProcessor[0]);
        for (BeanPostProcessor bp : bpps) {
            exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
        }
        return exposedObject;
    }

    protected void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) throws Exception {
        lifecycleManager.invokeInitMethods(beanName, bean, bd);
    }

    public void destroyBeans() {
        singletonCache.destroySingletons((beanName, bean) -> {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            lifecycleManager.invokeDestroyMethods(beanName, bean, bd);
        });
        for (Map.Entry<String, Object> entry : this.factoryBeanObjectCache.entrySet()) {
            String cacheKey = entry.getKey();
            Object obj = entry.getValue();
            if (obj != null) {
                String factoryBeanName = cacheKey.contains("_$") ?
                        cacheKey.substring(0, cacheKey.indexOf("_$")) : cacheKey;
                lifecycleManager.invokeDestroyMethods(factoryBeanName + "_factoryBeanObject", obj);
            }
        }
        this.factoryBeanObjectCache.clear();
        this.cachedAutowiredConstructors.clear();
        this.cachedDefaultConstructors.clear();
        this.cachedAnnotationMetadata.clear();
        this.cachedSetterMethods.clear();
        this.cachedBeanNamesForType.clear();
        this.typeIndex.clear();
        this.beanCreationTimes.clear();
        this.frozen = false;
        this.singletonsCreated = false;
        this.frozenBpps = new BeanPostProcessor[0];
        this.fastBeanLookup.clear();
        this.dependencyEdges.clear();
        logger.info("All singleton beans destroyed and caches cleared");
    }

    protected void invokeDestroyMethods(String beanName, Object bean) {
        lifecycleManager.invokeDestroyMethods(beanName, bean);
    }

    /**
     * 统一执行 BPP 方法 — 消除 applyBeanPostProcessorsBefore/AfterInitialization 的重复代码
     */
    private Object applyBeanPostProcessors(Object existingBean, String beanName,
                                            BppPhase phase) throws Exception {
        Object result = existingBean;
        BeanPostProcessor[] bpps = this.frozenBpps.length > 0 ? this.frozenBpps
                : this.beanPostProcessors.toArray(new BeanPostProcessor[0]);
        for (BeanPostProcessor bp : bpps) {
            Object current;
            if (phase == BppPhase.BEFORE) {
                current = bp.postProcessBeforeInitialization(result, beanName);
            } else {
                current = bp.postProcessAfterInitialization(result, beanName);
            }
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }

    protected Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws Exception {
        return applyBeanPostProcessors(existingBean, beanName, BppPhase.BEFORE);
    }

    protected Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws Exception {
        return applyBeanPostProcessors(existingBean, beanName, BppPhase.AFTER);
    }

    private enum BppPhase { BEFORE, AFTER }

    protected void addSingleton(String beanName, Object singletonObject) {
        singletonCache.addSingleton(beanName, singletonObject);
    }

    public void registerSingleton(String beanName, Object singletonObject) {
        singletonCache.addSingleton(beanName, singletonObject);
    }

    protected void addSingletonFactory(String beanName, SingletonCache.ObjectFactory<?> singletonFactory) {
        singletonCache.addSingletonFactory(beanName, singletonFactory);
    }

    public void beforeSingletonCreation(String beanName) {
        singletonCache.beforeSingletonCreation(beanName);
    }

    public void afterSingletonCreation(String beanName) {
        singletonCache.afterSingletonCreation(beanName);
    }

    protected boolean isSingletonCurrentlyInCreation(String beanName) {
        return singletonCache.isSingletonCurrentlyInCreation(beanName);
    }

    public void preInstantiateSingletons() throws Exception {
        List<String> allBeanNames = new ArrayList<>(this.beanDefinitionMap.keySet());
        List<String> beanNames = new ArrayList<>();
        for (String name : allBeanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(name);
            if (bd != null && bd.isSingleton() && !bd.isLazyInit()) {
                beanNames.add(name);
            }
        }
        beanNames = topologicalSort(beanNames);

        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd != null && bd.isSingleton() && !bd.isLazyInit()) {
                getBean(beanName);
            }
        }

        this.frozen = true;
        this.frozenBpps = this.beanPostProcessors.toArray(new BeanPostProcessor[0]);
        this.singletonsCreated = true;

        fastBeanLookup.build(singletonCache.getSingletonObjects());

        logger.info("Pre-instantiated {} singleton beans", singletonCache.getSingletonCount());
    }

    // 基于 @DependsOn 的拓扑排序，保证依赖项先实例化；循环依赖时跳过
    private List<String> topologicalSort(List<String> beanNames) {
        Map<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            Set<String> deps = new LinkedHashSet<>();
            if (bd != null && bd.getDependsOn() != null) {
                for (String dep : bd.getDependsOn()) {
                    if (beanNames.contains(dep)) {
                        deps.add(dep);
                    }
                }
            }
            dependencyGraph.put(beanName, deps);
        }

        List<String> sorted = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> inProgress = new LinkedHashSet<>();

        for (String beanName : beanNames) {
            if (!visited.contains(beanName)) {
                topologicalSortDFS(beanName, dependencyGraph, visited, inProgress, sorted);
            }
        }

        return sorted;
    }

    private void topologicalSortDFS(String beanName, Map<String, Set<String>> graph,
                                     Set<String> visited, Set<String> inProgress, List<String> sorted) {
        visited.add(beanName);
        inProgress.add(beanName);
        Set<String> deps = graph.get(beanName);
        if (deps != null) {
            for (String dep : deps) {
                if (!visited.contains(dep)) {
                    topologicalSortDFS(dep, graph, visited, inProgress, sorted);
                } else if (inProgress.contains(dep)) {
                    throw new IllegalStateException("Circular @DependsOn detected: " + beanName + " -> " + dep);
                }
            }
        }
        inProgress.remove(beanName);
        sorted.add(beanName);
    }

    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitionMap.get(beanName);
    }

    // BeanDefinitionRegistry interface implementation
    @Override
    public void removeBeanDefinition(String beanName) throws Exception {
        if (frozen) {
            throw new IllegalStateException("BeanFactory is frozen, cannot remove bean definition '" + beanName + "'");
        }
        BeanDefinition removed = this.beanDefinitionMap.remove(beanName);
        if (removed == null) {
            throw new NoSuchBeanDefinitionException(beanName);
        }
        this.cachedBeanNamesForType.clear();
        // Remove from typeIndex
        Class<?> beanClass = removed.getBeanClass();
        if (beanClass != null) {
            removeFromTypeIndexRecursive(beanClass, beanName);
        }
        // 如果 Bean 已经实例化为单例，也要从缓存中移除
        singletonCache.removeSingleton(beanName);
        if (logger.isDebugEnabled()) logger.debug("Removed bean definition: {}", beanName);
    }

    private void removeFromTypeIndexRecursive(Class<?> type, String beanName) {
        if (type == null || type == Object.class) return;
        removeFromTypeIndex(type, beanName);
        for (Class<?> iface : type.getInterfaces()) {
            removeFromTypeIndexRecursive(iface, beanName);
        }
        removeFromTypeIndexRecursive(type.getSuperclass(), beanName);
    }

    private void removeFromTypeIndex(Class<?> type, String beanName) {
        typeIndex.computeIfPresent(type, (k, list) -> {
            list.remove(beanName);
            return list.isEmpty() ? null : list;
        });
    }

    // Phase 3: dump dependency graph in Mermaid format
    public void dumpGraph(Appendable out) {
        try {
            out.append("```mermaid\ngraph TD\n");
            synchronized (this.dependencyEdges) {
                for (String[] edge : this.dependencyEdges) {
                    out.append("  ").append(edge[0]).append(" --> ").append(edge[1]).append("\n");
                }
            }
            out.append("```\n");
        } catch (java.io.IOException e) {
            logger.warn("Failed to dump dependency graph", e);
        }
    }

    // Phase 3: record a dependency edge for dumpGraph
    public void recordDependencyEdge(String from, String to) {
        this.dependencyEdges.add(new String[]{from, to});
    }

    // Phase 3: get singleton count
    public int getSingletonCount() {
        return singletonCache.getSingletonCount();
    }

    // Phase 3: get startup duration (set by ApplicationContext)
    private long startupDuration;

    public void setStartupDuration(long ms) {
        this.startupDuration = ms;
    }

    public long getStartupDuration() {
        return startupDuration;
    }

    // Bean 创建时间统计 API
    public long getBeanCreationTime(String beanName) {
        return beanCreationTimes.getOrDefault(beanName, -1L);
    }

    public Map<String, Long> getBeanCreationTimes() {
        return Collections.unmodifiableMap(beanCreationTimes);
    }

    // 打印最慢的 N 个 Bean 创建时间
    public void printSlowestBeans(int topN) {
        beanCreationTimes.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(topN)
            .forEach(e -> logger.info("Bean '{}' creation time: {} ms", e.getKey(), e.getValue() / 1_000_000.0));
    }

    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }

    public void setPropertyPlaceholderConfigurer(PropertyPlaceholderConfigurer configurer) {
        this.propertyPlaceholderConfigurer = configurer;
    }

    public PropertyPlaceholderConfigurer getPropertyPlaceholderConfigurer() {
        return this.propertyPlaceholderConfigurer;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public ApplicationEventPublisher getEventPublisher() {
        return this.eventPublisher;
    }

    public void publishEvent(ApplicationEvent event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }
}