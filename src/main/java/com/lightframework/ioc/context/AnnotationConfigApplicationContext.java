package com.lightframework.ioc.context;

import com.lightframework.di.annotation.Autowired;
import com.lightframework.di.annotation.Bean;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Configuration;
import com.lightframework.di.annotation.DependsOn;
import com.lightframework.ioc.annotation.EventListener;
import com.lightframework.di.annotation.Import;
import com.lightframework.di.annotation.Lazy;
import com.lightframework.di.annotation.Primary;
import com.lightframework.di.annotation.Profile;
import com.lightframework.di.annotation.Qualifier;
import com.lightframework.di.annotation.Scope;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.BeanFactoryPostProcessor;
import com.lightframework.ioc.core.BeanPostProcessor;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.Environment;
import com.lightframework.ioc.core.ImportBeanDefinitionRegistrar;
import com.lightframework.ioc.core.ImportSelector;
import com.lightframework.ioc.core.StandardEnvironment;
import com.lightframework.ioc.core.health.HealthCheckResult;
import com.lightframework.ioc.core.health.IoCHealthChecker;
import com.lightframework.ioc.event.ApplicationEvent;
import com.lightframework.ioc.event.ApplicationEventPublisher;
import com.lightframework.ioc.event.ContextClosedEvent;
import com.lightframework.ioc.event.SimpleApplicationEventMulticaster;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class AnnotationConfigApplicationContext implements ApplicationContext, ApplicationEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigApplicationContext.class);
    
    private final DefaultListableBeanFactory beanFactory;
    
    private final List<Class<?>> componentClasses = new ArrayList<>();
    
    private Environment environment = new StandardEnvironment();
    
    private String id = "light-framework-context";
    private String displayName = "AnnotationConfigApplicationContext";
    private long startupDate;
    private boolean active = false;
    private SimpleApplicationEventMulticaster applicationEventMulticaster;
    
    public AnnotationConfigApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }
    
    public AnnotationConfigApplicationContext(Class<?>... componentClasses) throws Exception {
        this();
        register(componentClasses);
        refresh();
    }
    
    public AnnotationConfigApplicationContext(String... basePackages) throws Exception {
        this();
        scan(basePackages);
        refresh();
    }
    
    public void register(Class<?>... componentClasses) {
        for (Class<?> componentClass : componentClasses) {
            this.componentClasses.add(componentClass);
        }
    }
    
    public void scan(String... basePackages) throws Exception {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.beanFactory);
        scanner.setEnvironment(this.environment);
        int count = scanner.scan(basePackages);
        logger.info("Scanned {} component classes from packages: {}", count, basePackages);
    }
    
    @Override
    public void refresh() throws Exception {
        this.startupDate = System.currentTimeMillis();
        this.active = true;
        
        // 1. 注册 BeanDefinition
        registerBeanDefinitions();
        
        // ★ 运行前健康检查 — 循环依赖检测与自动修复
        runIoCHealthCheck();
        
        // 2. SPI: 扫描 META-INF/lightssm.spi 自动配置
        discoverSpiAutoConfigurations();
        
        // 将 ApplicationContext 设置到 BeanFactory，用于 ApplicationContextAware 接口
        this.beanFactory.setApplicationContext(this);
        
        processImports();
        
        // Phase 4: 先执行 BeanDefinitionRegistryPostProcessor
        invokeBeanDefinitionRegistryPostProcessors();
        
        invokeBeanFactoryPostProcessors();
        
        // 初始化事件广播器
        initApplicationEventMulticaster();
        
        registerBeanPostProcessors();
        
        this.beanFactory.preInstantiateSingletons();
        
        // Phase 4: 处理 @Configuration 类中的 @Bean 方法（在 BPP 注册和预实例化之后）
        processBeanMethods();
        
        logger.info("ApplicationContext refreshed successfully, {} beans instantiated", 
            this.beanFactory.getBeanDefinitionCount());
    }
    
    /**
     * ★ 运行前健康检查 — 循环依赖检测与自动修复
     */
    private void runIoCHealthCheck() {
        try {
            HealthCheckResult result = IoCHealthChecker.run(this.beanFactory);
            if (!result.healthy()) {
                // 构造器循环未修复 → 抛出异常阻止启动
                if (result.hasUnfixedConstructorCycles()) {
                    throw new IllegalStateException(result.toReportString());
                }
                logger.warn(result.toReportString());
            } else if (result.autoFixedCount() > 0) {
                logger.info(result.toReportString());
            }
        } catch (IllegalStateException e) {
            throw e; // 致命错误，向上传播
        } catch (Exception e) {
            logger.warn("IoC health check failed (non-fatal): {}", e.getMessage());
        }
    }
    
    protected void registerBeanDefinitions() throws Exception {
        for (Class<?> componentClass : this.componentClasses) {
            registerComponent(componentClass);
        }
    }
    
    protected void registerComponent(Class<?> componentClass) {
        // Phase 4: 检查 @Conditional 注解
        if (!evaluateConditions(componentClass)) {
            logger.debug("Skipping bean {} - condition not met", componentClass.getName());
            return;
        }

        // 检查 @Profile 注解
        Profile profile = componentClass.getAnnotation(Profile.class);
        if (profile != null && !environment.acceptsProfiles(profile.value())) {
            logger.debug("Skipping bean {} - profile not active", componentClass.getName());
            return;
        }

        // Phase 4: 支持 @Configuration 注解
        Configuration config = componentClass.getAnnotation(Configuration.class);
        if (config != null) {
            registerConfigurationClass(componentClass);
            return;
        }

        // 查找 @Component 或其派生注解
        Component component = findComponentAnnotation(componentClass);
        if (component == null) {
            throw new IllegalArgumentException("Class " + componentClass.getName() + 
                " is not annotated with @Component or a derived annotation");
        }
        
        String beanName = component.value();
        if (beanName.isEmpty()) {
            beanName = generateBeanName(componentClass);
        }
        
        BeanDefinition bd = new BeanDefinition(beanName, componentClass);
        
        Scope scope = componentClass.getAnnotation(Scope.class);
        if (scope != null) {
            bd.setScope(scope.value());
        }
        
        if (componentClass.isAnnotationPresent(Primary.class)) {
            bd.setPrimary(true);
        }

        // 支持 @Qualifier 注解
        Qualifier qualifier = componentClass.getAnnotation(Qualifier.class);
        if (qualifier != null && !qualifier.value().isEmpty()) {
            bd.setQualifier(qualifier.value());
        }
        
        // 支持 @Lazy 注解
        Lazy lazy = componentClass.getAnnotation(Lazy.class);
        if (lazy != null) {
            bd.setLazyInit(lazy.value());
        }

        // 支持 @DependsOn 注解
        DependsOn dependsOn = componentClass.getAnnotation(DependsOn.class);
        if (dependsOn != null && dependsOn.value().length > 0) {
            bd.setDependsOn(dependsOn.value());
        }

        this.beanFactory.registerBeanDefinition(beanName, bd);
        logger.debug("Registered bean definition: {} -> {}", beanName, componentClass.getName());
    }
    
    protected void processImports() throws Exception {
        Set<String> processedClasses = new HashSet<>();
        int maxIterations = 100; // 防止无限循环
        int iteration = 0;
        boolean changed = true;
        while (changed && iteration < maxIterations) {
            changed = false;
            iteration++;
            String[] beanNames = this.beanFactory.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
                Class<?> beanClass = bd.getBeanClass();
                if (beanClass == null) {
                    continue;
                }
                Import importAnnotation = beanClass.getAnnotation(Import.class);
                if (importAnnotation != null) {
                    for (Class<?> importedClass : importAnnotation.value()) {
                        String className = importedClass.getName();
                        if (processedClasses.contains(className)) {
                            continue;
                        }
                        processedClasses.add(className);
                        changed = true;
                        processImportedClass(importedClass);
                    }
                }
            }
        }
        if (iteration >= maxIterations) {
            logger.warn("processImports reached max iterations ({}), possible circular @Import", maxIterations);
        }
    }
    
    protected void processImportedClass(Class<?> importedClass) throws Exception {
        // 1. 如果是 ImportSelector
        if (ImportSelector.class.isAssignableFrom(importedClass)) {
            java.lang.reflect.Constructor<?> selectorCtor = importedClass.getDeclaredConstructor();
            selectorCtor.setAccessible(true);
            ImportSelector selector = (ImportSelector) selectorCtor.newInstance();
            String[] imports = selector.selectImports();
            for (String importClassName : imports) {
                Class<?> clazz = Class.forName(importClassName);
                registerComponent(clazz);
            }
            return;
        }
        
        // 2. 如果是 ImportBeanDefinitionRegistrar
        if (ImportBeanDefinitionRegistrar.class.isAssignableFrom(importedClass)) {
            java.lang.reflect.Constructor<?> registrarCtor = importedClass.getDeclaredConstructor();
            registrarCtor.setAccessible(true);
            ImportBeanDefinitionRegistrar registrar = (ImportBeanDefinitionRegistrar) registrarCtor.newInstance();
            registrar.registerBeanDefinitions(this.beanFactory);
            return;
        }
        
        // 3. 普通配置类，直接注册
        if (importedClass.isAnnotationPresent(Component.class)) {
            registerComponent(importedClass);
        }
    }
    
    protected String generateBeanName(Class<?> beanClass) {
        String shortName = beanClass.getSimpleName();
        // 处理特殊情况：如 XMLParser -> xmlParser, URLHandler -> urlHandler
        if (shortName.length() > 1 && Character.isUpperCase(shortName.charAt(1)) 
                && Character.isUpperCase(shortName.charAt(0))) {
            return shortName; // 保持原样，如 "URL" -> "URL"
        }
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }
    
    /**
     * 查找 @Component 注解或其派生注解（如 @Service, @Repository）
     */
    private Component findComponentAnnotation(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        if (component != null) {
            return component;
        }
        for (java.lang.annotation.Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Component.class)) {
                return new Component() {
                    @Override
                    public String value() {
                        try {
                            return (String) ann.annotationType().getMethod("value").invoke(ann);
                        } catch (Exception e) {
                            return "";
                        }
                    }
                    @Override
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return Component.class;
                    }
                };
            }
        }
        return null;
    }

    /**
     * Phase 4: 评估 @Conditional 条件
     */
    protected boolean evaluateConditions(Class<?> componentClass) {
        com.lightframework.ioc.annotation.Conditional conditional = 
            componentClass.getAnnotation(com.lightframework.ioc.annotation.Conditional.class);
        if (conditional == null) {
            return true;  // 没有 @Conditional 注解，直接通过
        }
        
        // ★ SPI: 设置 ThreadLocal 供 OnClassCondition/OnMissingBeanCondition 读取
        com.lightframework.spi.condition.OnClassCondition.currentClassName.set(componentClass.getName());
        com.lightframework.spi.condition.OnMissingBeanCondition.currentClassName.set(componentClass.getName());
        
        try {
            com.lightframework.ioc.core.ConditionContext context = new com.lightframework.ioc.core.ConditionContext() {
                @Override
                public com.lightframework.ioc.core.BeanDefinitionRegistry getRegistry() {
                    return beanFactory;
                }
                
                @Override
                public Environment getEnvironment() {
                    return environment;
                }
                
                @Override
                public ClassLoader getClassLoader() {
                    return Thread.currentThread().getContextClassLoader();
                }
            };
            
            for (Class<? extends com.lightframework.ioc.core.Condition> conditionClass : conditional.value()) {
                try {
                    java.lang.reflect.Constructor<?> ctor = conditionClass.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    com.lightframework.ioc.core.Condition condition = 
                        (com.lightframework.ioc.core.Condition) ctor.newInstance();
                    if (!condition.matches(context)) {
                        return false;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to evaluate condition: {}", conditionClass.getName(), e);
                    return false;
                }
            }
            return true;
        } finally {
            com.lightframework.spi.condition.OnClassCondition.currentClassName.remove();
            com.lightframework.spi.condition.OnMissingBeanCondition.currentClassName.remove();
        }
    }

    /**
     * ★ SPI: 评估 @Bean 方法上的 @Conditional 条件
     */
    protected boolean evaluateMethodConditions(java.lang.reflect.Method method) {
        com.lightframework.ioc.annotation.Conditional conditional =
            method.getAnnotation(com.lightframework.ioc.annotation.Conditional.class);
        if (conditional == null) {
            return true;
        }
        com.lightframework.spi.condition.OnClassCondition.currentClassName.set(method.getDeclaringClass().getName());
        com.lightframework.spi.condition.OnMissingBeanCondition.currentClassName.set(method.getDeclaringClass().getName());
        try {
            com.lightframework.ioc.core.ConditionContext context = new com.lightframework.ioc.core.ConditionContext() {
                @Override
                public com.lightframework.ioc.core.BeanDefinitionRegistry getRegistry() {
                    return beanFactory;
                }
                @Override
                public Environment getEnvironment() {
                    return environment;
                }
                @Override
                public ClassLoader getClassLoader() {
                    return Thread.currentThread().getContextClassLoader();
                }
            };
            for (Class<? extends com.lightframework.ioc.core.Condition> conditionClass : conditional.value()) {
                try {
                    java.lang.reflect.Constructor<?> ctor = conditionClass.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    com.lightframework.ioc.core.Condition condition =
                        (com.lightframework.ioc.core.Condition) ctor.newInstance();
                    if (!condition.matches(context)) {
                        return false;
                    }
                } catch (Exception e) {
                    logger.warn("Failed to evaluate condition on @Bean method: {}", method.getName(), e);
                    return false;
                }
            }
            return true;
        } finally {
            com.lightframework.spi.condition.OnClassCondition.currentClassName.remove();
            com.lightframework.spi.condition.OnMissingBeanCondition.currentClassName.remove();
        }
    }

    /**
     * ★ SPI: 扫描 META-INF/lightssm.spi 自动配置类
     */
    protected void discoverSpiAutoConfigurations() throws Exception {
        Enumeration<URL> urls = getClassLoader().getResources("META-INF/lightssm.spi");
        if (!urls.hasMoreElements()) {
            return;
        }
        logger.info("Scanning META-INF/lightssm.spi for SPI auto-configurations...");
        while (urls.hasMoreElements()) {
            try (InputStream is = urls.nextElement().openStream();
                 Scanner sc = new Scanner(is, "UTF-8")) {
                while (sc.hasNextLine()) {
                    String line = sc.nextLine().trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    try {
                        Class<?> configClass = Class.forName(line, false, getClassLoader());
                        // 检查 @Conditional 条件
                        if (evaluateConditions(configClass)) {
                            registerComponent(configClass);
                            logger.debug("Registered SPI auto-configuration: {}", line);
                        } else {
                            logger.debug("Skipped SPI auto-configuration {} - condition not met", line);
                        }
                    } catch (ClassNotFoundException e) {
                        logger.debug("SPI auto-configuration class not found: {}", line);
                    }
                }
            }
        }
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        return cl;
    }

    /**
     * Phase 4: 注册 @Configuration 类，延迟处理 @Bean 方法到 preInstantiateSingletons 之后
     */
    protected void registerConfigurationClass(Class<?> configClass) {
        Configuration config = configClass.getAnnotation(Configuration.class);
        String beanName = config.value();
        if (beanName.isEmpty()) {
            beanName = generateBeanName(configClass);
        }
        
        // 只注册配置类本身，不立即实例化
        BeanDefinition bd = new BeanDefinition(beanName, configClass);
        this.beanFactory.registerBeanDefinition(beanName, bd);
        logger.debug("Registered configuration class: {}", beanName);
        
        // 延迟处理 @Bean 方法到预实例化阶段之后
        // 在 refresh 方法的 preInstantiateSingletons 之后调用
    }

    /**
     * Phase 4: 注册 @Bean 方法返回的对象
     */
    protected void registerBeanMethod(Object configInstance, java.lang.reflect.Method method, 
                                      Bean beanAnnotation) {
        String[] names = beanAnnotation.name().length > 0 ? beanAnnotation.name() : beanAnnotation.value();
        String beanName = names.length > 0 ? names[0] : method.getName();
        
        // ★ SPI: 评估 @Bean 方法上的 @Conditional 条件
        if (!evaluateMethodConditions(method)) {
            logger.debug("Skipping @Bean method {} - condition not met", method.getName());
            return;
        }
        
        // 检查是否已注册（避免重复处理）
        if (this.beanFactory.containsBeanDefinition(beanName)) {
            return;
        }
        
        try {
            // 解析方法参数（依赖注入，支持 @Qualifier 和 @Lazy）
            Class<?>[] paramTypes = method.getParameterTypes();
            java.lang.annotation.Annotation[][] paramAnnotations = method.getParameterAnnotations();
            Object[] args = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                String qualifier = null;
                boolean isLazy = false;
                for (java.lang.annotation.Annotation ann : paramAnnotations[i]) {
                    if (ann instanceof Qualifier) {
                        qualifier = ((Qualifier) ann).value();
                    } else if (ann instanceof Lazy) {
                        isLazy = ((Lazy) ann).value();
                    }
                }
                if (isLazy) {
                    args[i] = this.beanFactory.createLazyProxy(paramTypes[i], qualifier);
                    if (args[i] == null) {
                        args[i] = this.beanFactory.resolveDependency(paramTypes[i], method.getName(), qualifier, true);
                    }
                } else {
                    args[i] = this.beanFactory.resolveDependency(paramTypes[i], method.getName(), qualifier, true);
                }
            }
            
            method.setAccessible(true);
            Object beanInstance = method.invoke(configInstance, args);
            if (beanInstance == null) {
                return;
            }
            
            // 注册 BeanDefinition
            com.lightframework.ioc.beans.BeanDefinition bd = 
                new com.lightframework.ioc.beans.BeanDefinition(beanName, beanInstance.getClass());
            bd.setScope("singleton");
            // ★ 支持 @Bean(initMethod/destroyMethod)
            if (!beanAnnotation.initMethod().isEmpty()) {
                bd.setInitMethodName(beanAnnotation.initMethod());
            }
            if (!beanAnnotation.destroyMethod().isEmpty() && 
                !"(inferred)".equals(beanAnnotation.destroyMethod())) {
                bd.setDestroyMethodName(beanAnnotation.destroyMethod());
            }
            this.beanFactory.registerBeanDefinition(beanName, bd);
            
            // 注册别名
            for (int i = 1; i < names.length; i++) {
                this.beanFactory.registerAlias(beanName, names[i]);
            }
            
            // 对 @Bean 创建的对象应用依赖注入（@Autowired/@Resource 字段）和生命周期回调
            this.beanFactory.beforeSingletonCreation(beanName);
            Object initializedBean;
            try {
                this.beanFactory.populateBean(beanName, bd, beanInstance);
                initializedBean = this.beanFactory.initializeBean(beanName, beanInstance, bd);
                // 缓存初始化后的实例到单例缓存
                this.beanFactory.registerSingleton(beanName, initializedBean);
            } finally {
                this.beanFactory.afterSingletonCreation(beanName);
            }
            
            logger.debug("Registered @Bean: {} -> {}", beanName, initializedBean.getClass().getName());
        } catch (Exception e) {
            logger.warn("Failed to register @Bean method {}: {}", method.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * Phase 4: 处理所有 @Configuration 类中的 @Bean 方法
     */
    protected void processBeanMethods() {
        String[] beanNames = this.beanFactory.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
            if (bd == null) continue;
            Class<?> beanClass = bd.getBeanClass();
            if (beanClass == null) continue;
            Configuration config = beanClass.getAnnotation(Configuration.class);
            if (config != null) {
                try {
                    Object configInstance = this.beanFactory.getBean(beanName, beanClass);
                    Object configProxy = createConfigurationProxy(configInstance, beanClass, beanName);
                    for (java.lang.reflect.Method method : beanClass.getDeclaredMethods()) {
                        Bean beanAnnotation = method.getAnnotation(Bean.class);
                        if (beanAnnotation != null) {
                            registerBeanMethod(configProxy, method, beanAnnotation);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Failed to process @Bean methods in {}: {}", beanClass.getName(), e.getMessage());
                }
            }
        }
    }

    private Object createConfigurationProxy(Object configInstance, Class<?> configClass, String beanName) {
        Map<String, Object> beanMethodCache = new HashMap<>();
        net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
        enhancer.setSuperclass(configClass);
        enhancer.setCallback((net.sf.cglib.proxy.MethodInterceptor) (obj, method, args, proxy) -> {
            Bean beanAnn = method.getAnnotation(Bean.class);
            if (beanAnn != null) {
                String[] names = beanAnn.name().length > 0 ? beanAnn.name() : beanAnn.value();
                String methodBeanName = names.length > 0 ? names[0] : method.getName();
                Object cached = beanMethodCache.get(methodBeanName);
                if (cached != null) return cached;
                Object result = ((net.sf.cglib.proxy.MethodProxy) proxy).invokeSuper(obj, args);
                beanMethodCache.put(methodBeanName, result);
                return result;
            }
            return ((net.sf.cglib.proxy.MethodProxy) proxy).invokeSuper(obj, args);
        });
        Object proxy = enhancer.create();
        // 将原始实例的字段值复制到代理对象，保证 @Bean 方法内访问的字段状态一致
        copyFields(configInstance, configClass, proxy);
        // 用代理对象替换单例缓存，确保其他 Bean 注入的是代理
        this.beanFactory.registerSingleton(beanName, proxy);
        return proxy;
    }

    private void copyFields(Object source, Class<?> clazz, Object target) {
        if (clazz == null || clazz == Object.class) return;
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                field.set(target, field.get(source));
            } catch (Exception ignored) {
            }
        }
        copyFields(source, clazz.getSuperclass(), target);
    }
    
    protected void registerBeanPostProcessors() throws Exception {
        String[] postProcessorNames = this.beanFactory.getBeanNamesForType(BeanPostProcessor.class);
        for (String ppName : postProcessorNames) {
            BeanPostProcessor pp = this.beanFactory.getBean(ppName, BeanPostProcessor.class);
            this.beanFactory.addBeanPostProcessor(pp);
        }
    }

    protected void invokeBeanFactoryPostProcessors() throws Exception {
        String[] postProcessorNames = this.beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class);
        for (String ppName : postProcessorNames) {
            BeanFactoryPostProcessor pp = this.beanFactory.getBean(ppName, BeanFactoryPostProcessor.class);
            pp.postProcessBeanFactory(this.beanFactory);
        }
    }

    protected void invokeBeanDefinitionRegistryPostProcessors() throws Exception {
        String[] postProcessorNames = this.beanFactory.getBeanNamesForType(
                com.lightframework.ioc.core.BeanDefinitionRegistryPostProcessor.class);
        for (String ppName : postProcessorNames) {
            com.lightframework.ioc.core.BeanDefinitionRegistryPostProcessor pp = 
                this.beanFactory.getBean(ppName, com.lightframework.ioc.core.BeanDefinitionRegistryPostProcessor.class);
            pp.postProcessBeanDefinitionRegistry(this.beanFactory);
        }
    }
    
    // ==================== Event Publishing Support ====================
    
    protected void initApplicationEventMulticaster() throws Exception {
        applicationEventMulticaster = new SimpleApplicationEventMulticaster();
        // 将事件发布器设置到 BeanFactory，使 Bean 可以通过 BeanFactory 发布事件
        this.beanFactory.setEventPublisher(this);
        // 注册所有 ApplicationListener Bean
        try {
            String[] listenerNames = this.beanFactory.getBeanNamesForType(
                    com.lightframework.ioc.event.ApplicationListener.class);
            for (String name : listenerNames) {
                try {
                    com.lightframework.ioc.event.ApplicationListener<?> listener =
                            this.beanFactory.getBean(name, com.lightframework.ioc.event.ApplicationListener.class);
                    applicationEventMulticaster.addListener(listener);
                } catch (Exception e) {
                    logger.warn("Failed to create ApplicationListener bean '{}': {}", name, e.getMessage());
                }
            }
        } catch (Exception e) {
            // 忽略
        }
        // 注册 @EventListener 方法
        registerAnnotatedEventListeners();
    }
    
    private void registerAnnotatedEventListeners() {
        try {
            String[] beanNames = this.beanFactory.getBeanDefinitionNames();
            for (String beanName : beanNames) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
                Class<?> beanClass = bd != null ? bd.getBeanClass() : null;
                if (beanClass == null) {
                    continue;
                }
                // 扫描 bean 的公共方法，查找 @EventListener 注解
                for (Method method : beanClass.getMethods()) {
                    EventListener listenerAnnotation = method.getAnnotation(EventListener.class);
                    if (listenerAnnotation != null && method.getParameterCount() >= 1) {
                        Class<? extends ApplicationEvent> eventType = listenerAnnotation.value();
                        // 如果默认值是 ApplicationEvent.class，尝试从方法参数推断
                        if (eventType == ApplicationEvent.class && method.getParameterCount() == 1) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            if (ApplicationEvent.class.isAssignableFrom(paramType)) {
                                eventType = (Class<? extends ApplicationEvent>) paramType;
                            }
                        }
                        final String capturedBeanName = beanName;
                        final Class<? extends ApplicationEvent> finalEventType = eventType;
                        final Method finalMethod = method;
                        
                        applicationEventMulticaster.addListener(
                                new com.lightframework.ioc.event.ApplicationListener<ApplicationEvent>() {
                                    @Override
                                    public void onApplicationEvent(ApplicationEvent event) {
                                        if (finalEventType.isInstance(event)) {
                                            try {
                                                Object bean = beanFactory.getBean(capturedBeanName);
                                                finalMethod.setAccessible(true);
                                                if (finalMethod.getParameterCount() == 1) {
                                                    finalMethod.invoke(bean, event);
                                                }
                                            } catch (Exception e) {
                                                logger.warn("Error invoking @EventListener method: {}", method.getName(), e);
                                            }
                                        }
                                    }
                                }
                        );
                        logger.debug("Registered @EventListener method: {} on bean: {}", method.getName(), beanName);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error scanning @EventListener methods", e);
        }
    }
    
    @Override
    public void publishEvent(ApplicationEvent event) {
        if (applicationEventMulticaster != null) {
            applicationEventMulticaster.publishEvent(event);
        }
    }
    
    @Override
    public Object getBean(String name) throws Exception {
        return this.beanFactory.getBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws Exception {
        return this.beanFactory.getBean(name, requiredType);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        return this.beanFactory.getBean(requiredType);
    }
    
    @Override
    public boolean containsBean(String name) {
        return this.beanFactory.containsBean(name);
    }
    
    @Override
    public boolean isSingleton(String name) {
        return this.beanFactory.isSingleton(name);
    }
    
    @Override
    public boolean isPrototype(String name) {
        return this.beanFactory.isPrototype(name);
    }
    
    @Override
    public Class<?> getType(String name) {
        return this.beanFactory.getType(name);
    }
    
    @Override
    public String[] getAliases(String name) {
        return this.beanFactory.getAliases(name);
    }
    
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanFactory.containsBeanDefinition(beanName);
    }
    
    @Override
    public int getBeanDefinitionCount() {
        return this.beanFactory.getBeanDefinitionCount();
    }
    
    @Override
    public String[] getBeanDefinitionNames() {
        return this.beanFactory.getBeanDefinitionNames();
    }
    
    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return this.beanFactory.getBeanNamesForType(type);
    }
    
    @Override
    public <T> List<T> getBeansOfType(Class<T> type) throws Exception {
        return this.beanFactory.getBeansOfType(type);
    }
    
    @Override
    public <T> Map<String, T> getBeansOfTypeAsMap(Class<T> type) throws Exception {
        return this.beanFactory.getBeansOfTypeAsMap(type);
    }
    
    @Override
    public <T> T getPrimaryBean(Class<T> type) throws Exception {
        return this.beanFactory.getPrimaryBean(type);
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public String getApplicationName() {
        return "light-framework";
    }
    
    @Override
    public String getDisplayName() {
        return this.displayName;
    }
    
    @Override
    public long getStartupDate() {
        return this.startupDate;
    }
    
    @Override
    public ApplicationContext getParent() {
        return null;
    }
    
    @Override
    public void close() {
        publishEvent(new ContextClosedEvent(this));
        this.active = false;
        this.beanFactory.destroyBeans();
        logger.info("ApplicationContext closed");
    }
    
    @Override
    public boolean isActive() {
        return this.active;
    }
    
    public DefaultListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }
    
    // ==================== Environment Support ====================
    
    public Environment getEnvironment() {
        return environment;
    }
    
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}