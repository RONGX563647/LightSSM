# 02 - IoC容器核心实现与Spring源码对比

## 1. IoC容器概述

IoC（Inversion of Control，控制反转）是Spring框架的核心，它通过依赖注入（DI）实现了组件之间的解耦。

### 1.1 什么是IoC

IoC的核心思想是：将对象的创建、管理和依赖关系的维护交给容器，而不是由对象自己创建依赖。

传统方式：
```java
// 对象自己管理依赖
public class UserService {
    private UserDao userDao = new UserDaoImpl(); // 硬编码依赖
}
```

IoC方式：
```java
// 容器注入依赖
public class UserService {
    @Autowired
    private UserDao userDao; // 由容器注入
}
```

### 1.2 Spring官方的IoC架构

Spring官方的IoC容器是一个复杂的层次结构：

```
BeanFactory（基础接口）
├── HierarchicalBeanFactory（层次化能力）
├── ListableBeanFactory（列表能力）
│   └── ApplicationContext（应用上下文）
├── ConfigurableBeanFactory（可配置能力）
├── AutowireCapableBeanFactory（自动装配能力）
└── ConfigurableListableBeanFactory（完整能力）
    └── DefaultListableBeanFactory（核心实现）
```

### 1.3 LightSSM的简化架构

```
BeanFactory（基础接口）
├── ListableBeanFactory（列表能力）
└── DefaultListableBeanFactory（核心实现）

ApplicationContext（应用上下文）
└── AnnotationConfigApplicationContext（注解配置实现）
```

## 2. 核心接口设计

### 2.1 BeanFactory接口对比

**Spring官方源码**（spring-beans）：
```java
public interface BeanFactory {
    String FACTORY_BEAN_PREFIX = "&";
    
    Object getBean(String name) throws BeansException;
    <T> T getBean(String name, Class<T> requiredType) throws BeansException;
    <T> T getBean(Class<T> requiredType) throws BeansException;
    Object getBean(String name, Object... args) throws BeansException;
    boolean containsBean(String name);
    boolean isSingleton(String name) throws NoSuchBeanDefinitionException;
    boolean isPrototype(String name) throws NoSuchBeanDefinitionException;
    boolean isTypeMatch(String name, ResolvableType typeToMatch);
    Class<?> getType(String name);
    String[] getAliases(String name);
}
```

**LightSSM实现**：
```java
public interface BeanFactory {
    Object getBean(String name) throws Exception;
    <T> T getBean(String name, Class<T> requiredType) throws Exception;
    <T> T getBean(Class<T> requiredType) throws Exception;
    boolean containsBean(String name);
    boolean isSingleton(String name);
    boolean isPrototype(String name);
    Class<?> getType(String name);
    String[] getAliases(String name);
}
```

**关键差异分析**：

| 方法 | Spring官方 | LightSSM | 说明 |
|-----|-----------|----------|------|
| getBean参数 | 支持args构造注入 | 不支持 | LightSSM仅支持无参构造 |
| isTypeMatch | 支持ResolvableType | 不支持 | 泛型类型匹配 |
| 异常类型 | BeansException | Exception | 简化异常体系 |

### 2.2 ListableBeanFactory接口

**Spring官方**提供了10+个方法用于批量获取Bean：
```java
public interface ListableBeanFactory extends BeanFactory {
    boolean containsBeanDefinition(String beanName);
    int getBeanDefinitionCount();
    String[] getBeanDefinitionNames();
    String[] getBeanNamesForType(ResolvableType type);
    String[] getBeanNamesForType(Class<?> type);
    String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit);
    <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException;
    Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType);
    // ... 更多方法
}
```

**LightSSM简化版本**：
```java
public interface ListableBeanFactory extends BeanFactory {
    boolean containsBeanDefinition(String beanName);
    int getBeanDefinitionCount();
    String[] getBeanDefinitionNames();
    String[] getBeanNamesForType(Class<?> type);
    <T> List<T> getBeansOfType(Class<T> type) throws Exception;
    <T> T getPrimaryBean(Class<T> type) throws Exception;
}
```

## 3. 三级缓存解决循环依赖

### 3.1 什么是循环依赖

循环依赖是指两个或多个Bean互相引用对方，形成闭环：

```java
@Component("a")
public class A {
    @Autowired
    private B b;
}

@Component("b")
public class B {
    @Autowired
    private A a;
}
```

如果不做特殊处理，创建A需要B，创建B又需要A，导致无限递归。

### 3.2 Spring官方的三级缓存

Spring官方使用三级缓存来解决循环依赖问题：

```java
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry 
        implements SingletonBeanRegistry {
    
    // 一级缓存：存放完全初始化好的Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // 二级缓存：存放早期引用（已实例化但未填充属性）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    
    // 三级缓存：存放ObjectFactory，用于创建早期引用
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
    
    // 记录正在创建的Bean名称
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
}
```

### 3.3 LightSSM的三级缓存实现

**源码位置**：[DefaultListableBeanFactory.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L23-L34)

```java
public class DefaultListableBeanFactory implements ListableBeanFactory {
    // 一级缓存：完整Bean实例
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // 二级缓存：早期引用
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    
    // 三级缓存：ObjectFactory
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
    
    // 正在创建中的Bean集合
    private final Set<String> singletonsCurrentlyInCreation = 
        Collections.newSetFromMap(new ConcurrentHashMap<>(16));
}
```

**完全对应**：LightSSM的缓存结构与Spring官方完全一致，只是变量名略有不同。

### 3.4 三级缓存工作流程

**阶段一：实例化A**
```
1. 创建A的实例（newInstance）
2. 将A的ObjectFactory放入三级缓存
3. 将A加入singletonsCurrentlyInCreation集合
```

**阶段二：填充A的属性（发现需要B）**
```
4. 扫描A的@Autowired字段，发现需要注入B
5. 调用getBean(B.class)
6. 缓存中找不到B，开始创建B
```

**阶段三：实例化B**
```
7. 创建B的实例
8. 将B的ObjectFactory放入三级缓存
9. 将B加入singletonsCurrentlyInCreation集合
```

**阶段四：填充B的属性（发现需要A）**
```
10. 扫描B的@Autowired字段，发现需要注入A
11. 调用getBean(A.class)
12. 一级缓存找不到A
13. singletonsCurrentlyInCreation包含A，说明A正在创建
14. 从三级缓存获取A的ObjectFactory，调用getObject()得到早期引用
15. 将A的早期引用放入二级缓存，删除三级缓存
16. 返回A的早期引用给B
```

**阶段五：完成B的创建**
```
17. B的属性填充完成
18. B初始化完成
19. B放入一级缓存，清理二三级缓存
20. 返回B给A
```

**阶段六：完成A的创建**
```
21. A获得B的引用，属性填充完成
22. A初始化完成
23. A放入一级缓存，清理二三级缓存
```

### 3.5 核心源码解析

**获取单例Bean**（[DefaultListableBeanFactory.java:L167-186](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L167-L186)）：

```java
protected Object getSingleton(String beanName) {
    // 第一级：查找完整Bean
    Object singletonObject = this.singletonObjects.get(beanName);
    
    // 如果没找到且正在创建中
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 第二级：查找早期引用
        singletonObject = this.earlySingletonObjects.get(beanName);
        
        // 如果还没找到
        if (singletonObject == null) {
            // 第三级：查找ObjectFactory
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                // 通过工厂创建早期引用
                singletonObject = singletonFactory.getObject();
                
                // 提升到二级缓存
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
                
                logger.debug("Exposed early singleton bean: {}", beanName);
            }
        }
    }
    return singletonObject;
}
```

**添加单例Bean**（[DefaultListableBeanFactory.java:L321-327](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L321-L327)）：

```java
protected void addSingleton(String beanName, Object singletonObject) {
    this.singletonObjects.put(beanName, singletonObject);        // 放入一级缓存
    this.singletonFactories.remove(beanName);                    // 清理三级缓存
    this.earlySingletonObjects.remove(beanName);                 // 清理二级缓存
    this.createdBeanNames.add(beanName);                         // 记录已创建
    logger.debug("Added singleton bean to cache: {}", beanName);
}
```

### 3.6 为什么需要三级缓存？

很多面试题会问：为什么需要三级缓存？二级不够吗？

**答案**：三级缓存的核心作用是**支持AOP代理**。

在Spring官方实现中，`singletonFactories`存储的ObjectFactory会调用`getEarlyBeanReference()`方法，这个方法会执行：

```java
// Spring官方源码 AbstractAutowireCapableBeanFactory
protected Object getEarlyBeanReference(String beanName, RootBeanDefinition mbd, Object bean) {
    Object exposedObject = bean;
    if (mbd != null && !mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {
        // 执行BeanPostProcessor，可能返回代理对象
        exposedObject = applyBeanPostProcessorsAfterCreation(bean, beanName);
    }
    return exposedObject;
}
```

如果某个Bean需要被AOP代理，那么通过三级缓存返回的早期引用就是**代理对象**，而不是原始对象。

**LightSSM的实现**（[DefaultListableBeanFactory.java:L286-292](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L286-L292)）：

```java
protected Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
    Object exposedObject = bean;
    // 执行BeanPostProcessor的早期引用处理（可用于AOP）
    for (BeanPostProcessor bp : this.beanPostProcessors) {
        exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
    }
    return exposedObject;
}
```

## 4. Bean的生命周期

### 4.1 完整的Bean创建流程

**源码位置**：[DefaultListableBeanFactory.java:L198-229](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L198-L229)

```java
protected Object doCreateBean(String beanName, BeanDefinition bd) throws Exception {
    // 1. 检查是否正在创建（防止循环依赖）
    if (isSingletonCurrentlyInCreation(beanName)) {
        throw new BeanCurrentlyInCreationException(beanName);
    }
    
    // 2. 标记为正在创建
    beforeSingletonCreation(beanName);
    
    try {
        // 3. 实例化Bean
        Object beanInstance = instantiateBean(beanName, bd);
        
        // 4. 早期暴露引用（用于循环依赖）
        boolean earlySingletonExposure = bd.isSingleton() 
            && this.singletonsCurrentlyInCreation.contains(beanName);
        if (earlySingletonExposure) {
            addSingletonFactory(beanName, 
                () -> getEarlyBeanReference(beanName, bd, beanInstance));
        }
        
        // 5. 属性填充（依赖注入）
        populateBean(beanName, bd, beanInstance);
        
        // 6. 初始化Bean
        Object exposedObject = initializeBean(beanName, beanInstance, bd);
        
        // 7. 处理早期引用
        if (earlySingletonExposure) {
            Object earlySingletonReference = getSingleton(beanName);
            if (earlySingletonReference != null 
                && earlySingletonReference != beanInstance) {
                exposedObject = earlySingletonReference;
            }
        }
        
        return exposedObject;
    } finally {
        // 8. 清理创建标记
        afterSingletonCreation(beanName);
    }
}
```

### 4.2 实例化过程

**源码位置**：[DefaultListableBeanFactory.java:L231-240](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L231-L240)

```java
protected Object instantiateBean(String beanName, BeanDefinition bd) throws Exception {
    Class<?> beanClass = bd.getBeanClass();
    try {
        // 通过反射调用无参构造方法
        Object instance = beanClass.getDeclaredConstructor().newInstance();
        logger.debug("Instantiated bean: {}", beanName);
        return instance;
    } catch (Exception e) {
        throw new BeanCreationException(beanName, "Failed to instantiate bean", e);
    }
}
```

**Spring官方的实例化方式更丰富**：
```java
// 1. 通过Supplier
BeanDefinition bd = new RootBeanDefinition();
bd.setInstanceSupplier(() -> new MyBean());

// 2. 通过工厂方法
@Bean
public MyBean myBean() {
    return new MyBean();
}

// 3. 通过构造方法（LightSSM仅支持这种）
@Autowired
public MyBean(Dependency dep) { ... }
```

### 4.3 属性填充（依赖注入）

**源码位置**：[DefaultListableBeanFactory.java:L242-260](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L242-L260)

```java
protected void populateBean(String beanName, BeanDefinition bd, Object bean) throws Exception {
    Class<?> beanClass = bd.getBeanClass();
    Field[] fields = beanClass.getDeclaredFields();
    
    for (Field field : fields) {
        Autowired autowired = field.getAnnotation(Autowired.class);
        if (autowired != null) {
            // 解析依赖
            Object dependency = resolveDependency(
                field.getType(), field.getName(), autowired.required());
            
            if (dependency != null) {
                // 反射注入
                field.setAccessible(true);
                field.set(bean, dependency);
                logger.debug("Autowired field {} in bean {}", 
                    field.getName(), beanName);
            } else if (autowired.required()) {
                throw new BeanCreationException(beanName,
                    "Required dependency not found for field: " + field.getName());
            }
        }
    }
}
```

**依赖解析逻辑**（[DefaultListableBeanFactory.java:L274-284](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L274-L284)）：

```java
protected Object resolveDependency(Class<?> type, String fieldName, 
                                   boolean required) throws Exception {
    try {
        return getBean(type); // 递归获取依赖Bean
    } catch (NoSuchBeanDefinitionException e) {
        if (!required) {
            logger.debug("Optional dependency not found: {}#{}", 
                type.getSimpleName(), fieldName);
            return null; // 非必需依赖返回null
        }
        throw e; // 必需依赖抛出异常
    }
}
```

### 4.4 初始化过程

**源码位置**：[DefaultListableBeanFactory.java:L262-272](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L262-L272)

```java
protected Object initializeBean(String beanName, Object bean, BeanDefinition bd) throws Exception {
    Object wrappedBean = bean;
    
    // 1. 执行BeanPostProcessor前置处理
    wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
    
    // 2. 调用初始化方法（LightSSM为空实现）
    invokeInitMethods(beanName, wrappedBean, bd);
    
    // 3. 执行BeanPostProcessor后置处理
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    
    return wrappedBean;
}
```

**BeanPostProcessor执行**（[DefaultListableBeanFactory.java:L297-319](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L297-L319)）：

```java
protected Object applyBeanPostProcessorsBeforeInitialization(
        Object existingBean, String beanName) throws Exception {
    Object result = existingBean;
    for (BeanPostProcessor bp : this.beanPostProcessors) {
        Object current = bp.postProcessBeforeInitialization(result, beanName);
        if (current == null) {
            return result; // 返回null则使用原对象
        }
        result = current; // 可以替换Bean
    }
    return result;
}
```

## 5. BeanDefinition设计

### 5.1 BeanDefinition的作用

BeanDefinition是Bean的元数据描述，容器通过它来创建Bean实例。

### 5.2 LightSSM的BeanDefinition

**源码位置**：[BeanDefinition.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/beans/BeanDefinition.java)

```java
public class BeanDefinition {
    private String beanName;           // Bean名称
    private Class<?> beanClass;        // Bean类
    private String scope = "singleton"; // 作用域
    private boolean lazyInit = false;   // 是否懒加载
    private boolean primary = false;    // 是否主Bean
    
    // 构造函数、Getter/Setter...
}
```

### 5.3 Spring官方的BeanDefinition层次

```
BeanDefinition（接口）
├── AbstractBeanDefinition（抽象基类）
│   ├── RootBeanDefinition（原始定义）
│   ├── ChildBeanDefinition（子定义）
│   └── GenericBeanDefinition（通用定义）
└── AnnotatedBeanDefinition（注解定义）
    └── AnnotatedGenericBeanDefinition
```

**Spring官方的AbstractBeanDefinition包含50+个属性**：
```java
public abstract class AbstractBeanDefinition implements BeanDefinition {
    private volatile Object beanClass;           // Bean类
    private String scope = SCOPE_DEFAULT;        // 作用域
    private boolean abstractFlag = false;        // 是否抽象
    private boolean lazyInit = false;            // 懒加载
    private int autowireMode = AUTOWIRE_NO;      // 自动装配模式
    private int dependencyCheck = DEPENDENCY_CHECK_ALL; // 依赖检查
    private String[] dependsOn;                  // 依赖的Bean
    private boolean autowireCandidate = true;    // 是否候选
    private boolean primary = false;             // 是否主Bean
    private String initMethodName;               // 初始化方法
    private String destroyMethodName;            // 销毁方法
    // ... 更多属性
}
```

## 6. 类扫描与注册

### 6.1 ClassPathBeanDefinitionScanner

**源码位置**：[ClassPathBeanDefinitionScanner.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/context/ClassPathBeanDefinitionScanner.java)

扫描器负责扫描指定包下的类，识别带有@Component注解的类并注册为BeanDefinition。

核心流程：
```
1. 将包名转换为路径（com.example -> com/example）
2. 通过ClassLoader获取资源URL
3. 遍历目录或Jar包中的类文件
4. 加载类并检查@Component注解
5. 生成Bean名称
6. 创建BeanDefinition并注册
```

### 6.2 Bean名称生成策略

**源码位置**：[AnnotationConfigApplicationContext.java:L103-106](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/context/AnnotationConfigApplicationContext.java#L103-L106)

```java
protected String generateBeanName(Class<?> beanClass) {
    String shortName = beanClass.getSimpleName();
    // 首字母小写：UserService -> userService
    return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
}
```

这与Spring官方的`AnnotationBeanNameGenerator`策略一致。

## 7. ApplicationContext的refresh流程

### 7.1 Spring官方的refresh方法

Spring官方的`AbstractApplicationContext.refresh()`包含12个步骤：

```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        prepareRefresh();                    // 1. 准备刷新
        ConfigurableListableBeanFactory bf = obtainFreshBeanFactory();  // 2. 获取BeanFactory
        prepareBeanFactory(bf);              // 3. 配置BeanFactory
        postProcessBeanFactory(bf);          // 4. 后置处理
        invokeBeanFactoryPostProcessors(bf); // 5. 调用BFPP
        registerBeanPostProcessors(bf);      // 6. 注册BPP
        initMessageSource();                 // 7. 初始化消息源
        initApplicationEventMulticaster();   // 8. 初始化事件广播
        onRefresh();                         // 9. 模板方法
        registerListeners();                 // 10. 注册监听器
        finishBeanFactoryInitialization(bf); // 11. 初始化单例Bean
        finishRefresh();                     // 12. 完成刷新
    }
}
```

### 7.2 LightSSM的简化refresh

**源码位置**：[AnnotationConfigApplicationContext.java:L60-72](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/context/AnnotationConfigApplicationContext.java#L60-L72)

```java
@Override
public void refresh() throws Exception {
    this.startupDate = System.currentTimeMillis();
    this.active = true;
    
    // 1. 注册Bean定义
    registerBeanDefinitions();
    
    // 2. 注册BeanPostProcessor
    registerBeanPostProcessors();
    
    // 3. 预实例化单例Bean
    this.beanFactory.preInstantiateSingletons();
    
    logger.info("ApplicationContext refreshed successfully, {} beans instantiated", 
        this.beanFactory.getBeanDefinitionCount());
}
```

**简化对比**：

| 步骤 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 准备刷新 | 环境变量、监听器 | 设置时间戳和状态 |
| BeanFactory | 创建并配置 | 构造函数已创建 |
| BFPP调用 | 支持 | 不支持 |
| 事件系统 | 完整实现 | 不支持 |
| 消息源 | 国际化支持 | 不支持 |
| Bean初始化 | 条件触发 | 直接预实例化 |

## 8. 预实例化单例Bean

### 8.1 作用

容器启动时预实例化所有非懒加载的单例Bean，好处：
- 尽早发现配置错误
- 避免首次请求延迟
- 提前完成依赖注入

### 8.2 实现

**源码位置**：[DefaultListableBeanFactory.java:L349-360](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/ioc/core/DefaultListableBeanFactory.java#L349-L360)

```java
public void preInstantiateSingletons() throws Exception {
    List<String> beanNames = new ArrayList<>(this.beanDefinitionMap.keySet());
    
    for (String beanName : beanNames) {
        BeanDefinition bd = this.beanDefinitionMap.get(beanName);
        if (bd != null && bd.isSingleton() && !bd.isLazyInit()) {
            getBean(beanName); // 触发Bean创建
        }
    }
    
    logger.info("Pre-instantiated {} singleton beans", 
        this.singletonObjects.size());
}
```

## 9. 异常体系

### 9.1 LightSSM的异常层次

```
BeansException（基础异常）
├── BeanCreationException（创建异常）
├── BeanCurrentlyInCreationException（循环依赖异常）
└── NoSuchBeanDefinitionException（找不到Bean异常）
```

### 9.2 与Spring官方对比

Spring官方的异常体系更完整：
```
BeansException（100+子类）
├── BeanCreationException
│   ├── UnsatisfiedDependencyException
│   ├── BeanInstantiationException
│   └── ...
├── BeanNotOfRequiredTypeException
├── NoSuchBeanDefinitionException
│   └── NoUniqueBeanDefinitionException
└── ...
```

## 10. 设计模式总结

### 10.1 工厂模式

```java
// 产品接口
public interface BeanFactory { ... }

// 具体工厂
public class DefaultListableBeanFactory implements ListableBeanFactory { ... }
```

### 10.2 注册表模式

```java
// BeanDefinition注册表
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);

// 单例注册表
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
```

### 10.3 模板方法模式

```java
// 模板方法
protected Object doCreateBean(String beanName, BeanDefinition bd) throws Exception {
    // 定义算法骨架
    instantiateBean(...);
    populateBean(...);
    initializeBean(...);
}
```

### 10.4 策略模式

```java
// ObjectFactory策略
@FunctionalInterface
public interface ObjectFactory<T> {
    T getObject() throws Exception;
}
```

## 11. 面试考点

### 11.1 高频问题

1. **Spring如何解决循环依赖？**
   - 三级缓存机制
   - 早期引用暴露

2. **Bean的生命周期有哪些步骤？**
   - 实例化、属性填充、初始化、销毁

3. **BeanFactory和ApplicationContext的区别？**
   - ApplicationContext是高级容器，提供更多企业功能

4. **@Autowired和@Resource的区别？**
   - @Autowired按类型注入，@Resource按名称注入

### 11.2 深度问题

1. **为什么需要三级缓存？二级不够吗？**
   - 三级缓存支持AOP代理的早期引用

2. **Spring的单例Bean是线程安全的吗？**
   - 不是，需要自己保证线程安全

3. **BeanPostProcessor的作用？**
   - 在Bean初始化前后执行自定义逻辑，是AOP的基础

## 12. 总结

LightSSM的IoC容器完整实现了Spring的核心机制：

- **三级缓存**：完美解决循环依赖
- **生命周期管理**：实例化、属性填充、初始化
- **注解支持**：@Component、@Autowired、@Scope
- **扩展点**：BeanPostProcessor

通过对照Spring官方源码，可以清晰看到LightSSM保留了核心逻辑，剥离了企业级特性，非常适合学习理解。

---

**上一篇**：[01 - LightSSM架构概述与设计哲学](01-LightSSM架构概述与设计哲学.md)

**下一篇**：[03 - AOP代理机制与Spring AOP源码对比](03-AOP代理机制与SpringAOP源码对比.md)
