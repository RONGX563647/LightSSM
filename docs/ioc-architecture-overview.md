# Light Framework IoC 框架架构总览

## 概述

Light Framework IoC 是一个轻量级、高性能的依赖注入框架，设计灵感来源于 Spring Framework，但在核心实现上进行了多项性能优化和架构精简。框架采用分层架构，将核心功能解耦为独立组件，并通过 SPI 机制实现可扩展性。

## 架构分层

### 整体架构视图

```mermaid
flowchart TB
    subgraph AppLayer["应用层"]
        AC["AnnotationConfigApplicationContext"]
    end

    subgraph CoreLayer["核心层"]
        DLBF["DefaultListableBeanFactory"]
        SC["SingletonCache<br/>三级缓存"]
        FBL["FastBeanLookup<br/>快速查找"]
        BLM["BeanLifecycleManager<br/>生命周期管理"]
        DTC["DefaultTypeConverter<br/>类型转换"]
        PPC["PropertyPlaceholderConfigurer<br/>属性占位符"]
    end

    subgraph SPI["SPI 扩展层"]
        BPP["BeanPostProcessor"]
        BFPP["BeanFactoryPostProcessor"]
        BDRPP["BeanDefinitionRegistryPostProcessor"]
        Cond["Condition"]
        Imp["ImportSelector / ImportRegistrar"]
        FB["FactoryBean"]
        TC["TypeConverter"]
    end

    subgraph ScanLayer["扫描层"]
        SCANNER["ClassPathBeanDefinitionScanner<br/>ASM 预扫描"]
    end

    subgraph EventLayer["事件层"]
        EM["SimpleApplicationEventMulticaster"]
        AL["ApplicationListener"]
        EL["@EventListener"]
    end

    subgraph AnnotationLayer["注解层"]
        COMP["@Component"]
        AUTO["@Autowired"]
        RES["@Resource"]
        VAL["@Value"]
        CONF["@Configuration / @Bean"]
        LAZY["@Lazy"]
        SCOPE["@Scope"]
        COND["@Conditional"]
        PROF["@Profile"]
    end

    AC --> DLBF
    AC --> SCANNER
    AC --> EM
    DLBF --> SC
    DLBF --> FBL
    DLBF --> BLM
    DLBF --> DTC
    DLBF --> PPC
    DLBF -.-> BPP
    DLBF -.-> BFPP
    DLBF -.-> Cond
    DLBF -.-> Imp
    DLBF -.-> FB
    DLBF -.-> TC
    SCANNER --> AnnotationLayer
    EM --> AL
    EM --> EL
```

## 核心组件分析

### 1. DefaultListableBeanFactory — 核心 Bean 工厂

**职责**: 框架的心脏，负责 Bean 的注册、实例化、依赖注入、初始化和销毁。

**关键特性**:

| 特性 | 实现方式 |
|------|----------|
| 循环依赖解决 | 三级缓存（SingletonCache） |
| 依赖注入 | @Autowired / @Resource / @Value |
| 构造器注入 | @Autowired 构造器，支持 @Lazy 参数 |
| 字段注入 | MethodHandles 加速注入 |
| 方法注入 | Setter 方法注入 |
| 泛型注入 | List\<T\> / Map 类型支持 |
| 懒加载代理 | JDK 动态代理 |
| FactoryBean | & 前缀获取工厂本身 |
| 类型索引 | 注册时增量构建，查询 O(1) |
| 快速查找 | 前 32 个热门 Bean 数组查找 |
| 拓扑排序 | @DependsOn 优先实例化 |
| 工厂冻结 | preInstantiateSingletons 后锁定 |

**Bean 创建流程**:

```mermaid
flowchart TD
    accTitle: Bean 创建生命周期流程
    accDescr: 从 BeanDefinition 注册到完全初始化 Bean 的完整流程，包括循环依赖解决和生命周期回调。

    A["注册 BeanDefinition"] --> B["拓扑排序 @DependsOn"]
    B --> C["依赖项预初始化 getBean"]
    C --> D["beforeSingletonCreation<br/>标记创建中"]
    D --> E["实例化 instantiateBean<br/>@Autowired 构造器"]
    E --> F["applyPropertyValues<br/>SPI 属性注入"]
    F --> G["提前暴露三级缓存<br/>addSingletonFactory"]
    G --> H["populateBean<br/>依赖注入"]
    H --> I["initializeBean<br/>初始化"]
    
    subgraph InitPhase["初始化阶段"]
        I --> I1["BPP Before<br/>前置处理"]
        I1 --> I2["Aware 接口<br/>setBeanName/setBeanFactory"]
        I2 --> I3["@PostConstruct<br/>InitializingBean"]
        I3 --> I4["BPP After<br/>后置处理/代理"]
    end
    
    I4 --> J["earlySingletonReference<br/>循环引用检查"]
    J --> K["addSingleton 一级缓存"]
    K --> L["afterSingletonCreation<br/>标记完成"]
```

**性能优化亮点**:

| 优化项 | 策略 |
|--------|------|
| 反射缓存 | @Autowired 构造器、默认构造器、注解元数据、setter 方法全部 ConcurrentHashMap 缓存 |
| MethodHandles | 字段注入优先使用 MethodHandle.unreflectSetter，比反射快 |
| 类型索引 | registerBeanDefinition 时增量构建 typeIndex，避免全量遍历 |
| 快速查找表 | singletonsCreated 后构建前 32 个 Bean 的 hash+数组查找表 |
| 冻结 BPP 数组 | preInstantiateSingletons 后将 CopyOnWriteArrayList 转为数组 |
| Aware 位图 | AnnotationMetadata.awareFlags 用 byte 位图替代 instanceof 链 |
| BitSet 注入跟踪 | 防止父类/子类同名字段重复注入 |
| ASM 预扫描 | 扫描时不触发 Class.forName，直接读取字节码判断 @Component |

### 2. SingletonCache — 三级缓存管理

**职责**: 管理单例 Bean 的三级缓存，解决循环依赖。

```mermaid
flowchart LR
    accTitle: 三级缓存查找流程
    accDescr: getBean 时的三级缓存查找顺序：一级缓存 → 二级缓存 → 三级缓存工厂。

    A["getBean(name)"] --> B{"一级缓存<br/>singletonObjects"}
    B -->|"命中"| C["返回完全初始化 Bean"]
    B -->|"未命中<br/>且正在创建"| D{"二级缓存<br/>earlySingletonObjects"}
    D -->|"命中"| E["返回早期引用"]
    D -->|"未命中"| F{"三级缓存<br/>singletonFactories"}
    F -->|"命中"| G["getObject()<br/>创建早期引用"]
    G --> H["放入二级缓存<br/>移除三级缓存"]
    F -->|"未命中"| I["返回 null<br/>继续创建流程"]
```

**三级缓存结构**:

| 缓存级别 | 数据结构 | 用途 |
|----------|----------|------|
| 一级缓存 | singletonObjects | 完全初始化的单例 Bean |
| 二级缓存 | earlySingletonObjects | 早期暴露的半成品 Bean（未完成依赖注入和初始化） |
| 三级缓存 | singletonFactories | 单例工厂（ObjectFactory），用于创建早期引用 |

**循环依赖检测**: 使用 ThreadLocal\<Deque\<String\>\> 记录创建调用栈，检测到重复 Bean 时构建完整的循环依赖链路信息。

### 3. BeanLifecycleManager — 生命周期管理

**职责**: 管理 Bean 的初始化和销毁回调。

**初始化顺序**:

```mermaid
flowchart LR
    accTitle: Bean 初始化回调顺序
    accDescr: Bean 初始化时各回调方法的执行顺序。

    A["@PostConstruct<br/>方法"] --> B["InitializingBean<br/>.afterPropertiesSet()"]
```

**销毁顺序**:

```mermaid
flowchart LR
    accTitle: Bean 销毁回调顺序
    accDescr: Bean 销毁时各回调方法的执行顺序。

    A["@PreDestroy<br/>方法"] --> B["DisposableBean<br/>.destroy()"]
```

**性能优化**: @PostConstruct / @PreDestroy 方法按 Class 缓存，避免重复反射扫描。从父类到子类递归收集（先父后子）。

### 4. ClassPathBeanDefinitionScanner — 类路径扫描器

**职责**: 扫描指定包路径下的 @Component 类并注册为 BeanDefinition。

**核心特性**:

| 特性 | 说明 |
|------|------|
| ASM 预扫描 | 使用 objectweb.asm 直接读取 class 字节码，无需 Class.forName 即可判断是否有 @Component |
| 缓存组件列表 | 启动时读取 META-INF/lightssm.components 缓存，命中后跳过 ASM 扫描 |
| 并行扫描 | 支持 parallelScan 模式，多包并行扫描 |
| JAR 包支持 | 支持 file:// 和 jar:// 协议 |
| Profile 过滤 | 扫描时根据 Environment 过滤 @Profile 不匹配的类 |

**扫描流程**:

```mermaid
flowchart TD
    accTitle: 类路径扫描流程
    accDescr: 从包路径扫描到 BeanDefinition 注册的完整流程。

    A["扫描包路径"] --> B{"协议类型"}
    B -->|"file://"| C["遍历目录 .class 文件"]
    B -->|"jar://"| D["遍历 JarEntry"]
    C --> E["读取 class 字节码"]
    D --> E
    E --> F{"META-INF/lightssm.components<br/>缓存存在?"}
    F -->|"是"| G["检查 className 是否在缓存中"]
    F -->|"否"| H["ASM 扫描 @Component 注解"]
    G -->|"不在"| I["跳过"]
    G -->|"在"| J["Class.forName 加载"]
    H -->|"无注解"| I
    H -->|"有注解"| J
    J --> K["检查 @Profile"]
    K -->|"不匹配"| I
    K -->|"匹配"| L["构建 BeanDefinition"]
    L --> M["注册到 BeanFactory"]
```

### 5. FastBeanLookup — 快速 Bean 查找

**职责**: 在所有单例创建完成后，提供 O(1) 数组级别的 Bean 查找。

**实现原理**: 将前 32 个 Bean 的 name.hashCode、name、bean 实例存储为平行数组，通过 hash 比对 + equals 实现快速查找，避免 HashMap 的开销。

### 6. DefaultTypeConverter — 类型转换器

**职责**: 为 @Value 注入提供 String 到目标类型的转换。

**支持类型**:

| 类型 | 转换方式 |
|------|----------|
| 基本类型及包装类 | Integer, Long, Boolean, Double, Float, Short, Byte, Character |
| 枚举类型 | Enum.valueOf |
| 自定义类型 | 通过 registerConverter 注册 |

**设计模式**: 策略模式 + Copy-on-Write 不可变表。内置转换器表为不可变 Map，自定义转换器存储在 ConcurrentHashMap 中。

### 7. PropertyPlaceholderConfigurer — 属性占位符解析

**职责**: 解析 ${key} 和 ${key:defaultValue} 占位符。

**解析优先级**:
1. 加载的 properties 文件
2. System.getProperty(key)
3. System.getenv(key)
4. 默认值（如果提供）

**实现**: 作为 BeanFactoryPostProcessor 执行，在 Bean 实例化前加载属性并设置到 BeanFactory。

### 8. SimpleApplicationEventMulticaster — 事件发布器

**职责**: 实现 ApplicationEventPublisher 接口，支持同步/异步事件发布。

**特性**:
- 支持 ApplicationListener\<T\> 接口注册
- 支持 @EventListener 方法扫描注册
- 通过泛型类型推断自动匹配事件类型
- 可配置 Executor 实现异步事件处理

## 注解系统

### 核心注解

```mermaid
classDiagram
    accTitle: IoC 注解体系
    accDescr: 核心注解及其用途的类图。

    class Component {
        +String value()
    }
    class Autowired {
        +boolean required()
    }
    class Resource {
        +String name()
        +Class type()
    }
    class Value {
        +String value()
    }
    class Qualifier {
        +String value()
    }
    class Lazy {
        +boolean value()
    }
    class Scope {
        +String value()
    }
    class Primary {
    }
    class DependsOn {
        +String[] value()
    }
    class Conditional {
        +Class~? extends Condition~[] value()
    }
    class Profile {
        +String[] value()
    }
    class Import {
        +Class~?~[] value()
    }
    class Bean {
        +String[] name()
        +String[] value()
    }
    class Configuration {
        +String value()
    }
    class EventListener {
        +Class~? extends ApplicationEvent~ value()
    }

    Component --> Autowired : "注入"
    Component --> Resource : "注入"
    Component --> Value : "占位符注入"
    Component --> Qualifier : "消除歧义"
    Component --> Lazy : "懒加载"
    Component --> Scope : "作用域"
    Component --> Primary : "首选 Bean"
    Component --> DependsOn : "依赖顺序"
    Component --> Conditional : "条件注册"
    Component --> Profile : "环境过滤"
    Configuration --> Bean : "声明式 Bean"
    Component --> Import : "导入配置"
    Component --> EventListener : "事件监听"
```

### 注解对照表

| 注解 | 用途 | 支持的注入目标 |
|------|------|----------------|
| @Component | 组件声明 | 类 |
| @Autowired | 自动装配（按类型） | 字段、方法、构造器参数 |
| @Resource | 资源注入（按名称优先） | 字段、方法 |
| @Value | 占位符注入 | 字段 |
| @Qualifier | 消除类型歧义 | 字段、构造器参数、方法参数、类 |
| @Lazy | 懒加载 | 类、字段、构造器参数 |
| @Scope | 作用域（singleton/prototype） | 类 |
| @Primary | 首选 Bean | 类 |
| @DependsOn | 依赖顺序 | 类 |
| @Conditional | 条件注册 | 类、@Bean 方法 |
| @Profile | 环境过滤 | 类 |
| @Import | 导入配置类 | 类 |
| @Configuration | 配置类 | 类 |
| @Bean | 声明式 Bean | 方法 |
| @EventListener | 事件监听 | 方法 |

## SPI 扩展机制

### 扩展点一览

```mermaid
classDiagram
    accTitle: SPI 扩展接口体系
    accDescr: 框架提供的 SPI 扩展接口及其继承关系。

    class Aware {
        <<interface>>
    }
    class BeanNameAware {
        <<interface>>
        +setBeanName(name)
    }
    class BeanFactoryAware {
        <<interface>>
        +setBeanFactory(factory)
    }
    class ApplicationContextAware {
        <<interface>>
        +setApplicationContext(ctx)
    }
    class BeanPostProcessor {
        <<interface>>
        +postProcessBeforeInitialization()
        +postProcessAfterInitialization()
        +getEarlyBeanReference()
    }
    class BeanFactoryPostProcessor {
        <<interface>>
        +postProcessBeanFactory(factory)
    }
    class BeanDefinitionRegistryPostProcessor {
        <<interface>>
        +postProcessBeanDefinitionRegistry(registry)
    }
    class Condition {
        <<interface>>
        +matches(context)
    }
    class ImportSelector {
        <<interface>>
        +selectImports()
    }
    class ImportBeanDefinitionRegistrar {
        <<interface>>
        +registerBeanDefinitions(registry)
    }
    class FactoryBean~T~ {
        <<interface>>
        +getObject()
        +getObjectType()
        +isSingleton()
    }
    class TypeConverter {
        <<interface>>
        +supports(targetType)
        +convert(source, targetType)
        +getOrder()
    }
    class BeanInjector~T~ {
        <<interface>>
        +create()
        +inject(instance, factory)
        +postConstruct(instance)
    }
    class BeanDefinitionRegistry {
        <<interface>>
        +registerBeanDefinition()
        +removeBeanDefinition()
        +getBeanDefinition()
        +registerAlias()
    }

    Aware <|-- BeanNameAware
    Aware <|-- BeanFactoryAware
    Aware <|-- ApplicationContextAware
    BeanFactoryPostProcessor <|-- BeanDefinitionRegistryPostProcessor
    BeanDefinitionRegistry --> BeanDefinitionRegistryPostProcessor : "参数"
    BeanFactoryPostProcessor --> DefaultListableBeanFactory : "参数"
```

### SPI 详细列表

| 扩展点 | 接口 | 执行时机 | 用途 |
|--------|------|----------|------|
| **BeanNameAware** | Aware | 初始化阶段 | 获取 Bean 名称 |
| **BeanFactoryAware** | Aware | 初始化阶段 | 获取 BeanFactory 引用 |
| **ApplicationContextAware** | Aware | 初始化阶段 | 获取 ApplicationContext 引用 |
| **BeanPostProcessor** | SPI | 每个 Bean 初始化前后 | 修改/代理 Bean 实例 |
| **BeanFactoryPostProcessor** | SPI | Bean 实例化前 | 修改 BeanFactory 配置 |
| **BeanDefinitionRegistryPostProcessor** | SPI | BeanFactoryPostProcessor 前 | 动态注册 BeanDefinition |
| **Condition** | SPI | 注册 Bean 前 | 条件化 Bean 注册 |
| **ImportSelector** | SPI | @Import 处理时 | 动态选择导入类 |
| **ImportBeanDefinitionRegistrar** | SPI | @Import 处理时 | 编程式注册 Bean |
| **FactoryBean** | SPI | getBean 时 | 自定义 Bean 创建逻辑 |
| **TypeConverter** | SPI | @Value 注入时 | 自定义类型转换 |
| **BeanInjector** | SPI | 编译时生成 | APT 生成的 DI 代码 |

### 自动配置 SPI

框架支持通过 `META-INF/lightssm.spi` 文件实现自动配置：

```
# META-INF/lightssm.spi
com.example.autoconfigure.DatabaseAutoConfiguration
com.example.autoconfigure.RedisAutoConfiguration
```

启动时自动扫描并注册这些配置类，同时支持 @Conditional 条件过滤。

## ApplicationContext 启动流程

```mermaid
sequenceDiagram
    accTitle: ApplicationContext 启动序列
    accDescr: AnnotationConfigApplicationContext 从创建到完全启动的完整序列。

    participant User as "用户代码"
    participant AC as "AnnotationConfigApplicationContext"
    participant Scanner as "ClassPathBeanDefinitionScanner"
    participant BF as "DefaultListBeanFactory"
    participant SPI as "SPI 扩展点"
    participant EM as "EventMulticaster"

    User->>AC: new AnnotationConfigApplicationContext(basePackages)
    AC->>AC: register() / scan()
    AC->>Scanner: scan(basePackages)
    Scanner->>BF: registerBeanDefinition()
    
    AC->>AC: refresh()
    AC->>AC: registerBeanDefinitions()
    AC->>AC: discoverSpiAutoConfigurations()
    AC->>BF: setApplicationContext(this)
    AC->>AC: processImports()
    
    AC->>AC: invokeBeanDefinitionRegistryPostProcessors()
    AC->>SPI: postProcessBeanDefinitionRegistry()
    
    AC->>AC: invokeBeanFactoryPostProcessors()
    AC->>SPI: postProcessBeanFactory()
    
    AC->>EM: initApplicationEventMulticaster()
    AC->>AC: registerBeanPostProcessors()
    AC->>BF: preInstantiateSingletons()
    
    Note over BF: 拓扑排序 → 创建所有单例
    
    AC->>AC: processBeanMethods()
    AC->>BF: 处理 @Configuration 中的 @Bean
    
    AC-->>User: 容器启动完成
```

## BeanDefinition 设计

**标志位压缩**: 使用 byte 位图存储布尔标志，节省内存。

```mermaid
classDiagram
    accTitle: BeanDefinition 数据结构
    accDescr: BeanDefinition 的属性和位图优化。

    class BeanDefinition {
        -String beanName
        -Class~?~ beanClass
        -byte flags (位图)
        -String[] dependsOn
        -String qualifier
        -Map~String,Object~ propertyValues
        +FLAG_SINGLETON = 0x01
        +FLAG_PRIMARY = 0x02
        +FLAG_LAZY_INIT = 0x04
    }
```

| 位 | 标志 | 含义 |
|----|------|------|
| 0x01 | FLAG_SINGLETON | 是否为单例（默认） |
| 0x02 | FLAG_PRIMARY | 是否为首选 Bean |
| 0x04 | FLAG_LAZY_INIT | 是否懒加载 |

## 依赖注入策略

### 注入优先级

```mermaid
flowchart TD
    accTitle: 依赖注入优先级
    accDescr: populateBean 方法中各注入策略的执行顺序和优先级。

    A["populateBean 开始"] --> B["获取/构建 AnnotationMetadata"]
    B --> C["注入 @Resource 字段"]
    C --> D["注入 @Resource 方法"]
    D --> E["注入 @Autowired 字段"]
    E --> F["注入 @Autowired 方法"]
    F --> G{"PropertyPlaceholderConfigurer<br/>已设置?"}
    G -->|"是"| H["注入 @Value 字段"]
    G -->|"否"| I["跳过 @Value"]
```

**@Resource vs @Autowired**:
- @Resource 优先注入（支持按名称查找）
- @Autowired 后注入（按类型查找）
- BitSet 防止重复注入（父类/子类同名字段）

### 构造器解析策略

```mermaid
flowchart TD
    accTitle: 构造器解析策略
    accDescr: instantiateBean 方法中构造器选择和参数解析流程。

    A["instantiateBean"] --> B{"缓存中是否有<br/>@Autowired 构造器?"}
    B -->|"有"| C["使用缓存的 @Autowired 构造器"]
    B -->|"无"| D["扫描声明的构造器"]
    D --> E{"@Autowired 构造器数量"}
    E -->|"0"| F["使用无参构造器"]
    E -->|"1"| G["使用该 @Autowired 构造器"]
    E -->|">1"| H["抛出异常<br/>Multiple @Autowired constructors"]
    C --> I["解析构造器参数"]
    G --> I
    I --> J{"参数有 @Lazy?"}
    J -->|"是"| K["创建 JDK 动态代理"]
    J -->|"否"| L["resolveDependency 获取依赖"]
    K --> M["调用构造器 newInstance"]
    L --> M
```

## 性能优化总览

| 优化项 | 实现位置 | 效果 |
|--------|----------|------|
| 反射缓存 | cachedAutowiredConstructors, cachedAnnotationMetadata | 避免重复反射扫描 |
| MethodHandles 注入 | createFieldInjector | 字段注入比反射快 |
| 类型索引 | typeIndex (ConcurrentHashMap) | getBeanNamesForType 从 O(n) 到 O(1) |
| 快速查找表 | FastBeanLookup (前32个Bean) | hot path 避免 HashMap.get() |
| 冻结 BPP 数组 | frozenBpps | 遍历 BeanPostProcessor 无同步开销 |
| Aware 位图 | AnnotationMetadata.awareFlags | 替代 instanceof 链 |
| BitSet 注入跟踪 | injectedFieldBits / injectedMethodBits | 防止重复注入，替代 HashSet |
| ASM 预扫描 | scanAnnotationWithAsm | 避免 Class.forName 触发类加载 |
| 组件缓存 | META-INF/lightssm.components | 跳过 ASM 扫描 |
| Setter 缓存 | cachedSetterMethods | applyPropertyValues 避免重复反射 |

## 框架接口层次

```mermaid
classDiagram
    accTitle: 框架核心接口层次
    accDescr: BeanFactory → ListableBeanFactory → ApplicationContext 的接口继承关系。

    class BeanFactory {
        <<interface>>
        +getBean(name)
        +getBean(name, type)
        +getBean(type)
        +containsBean(name)
        +isSingleton(name)
        +isPrototype(name)
        +getType(name)
        +getAliases(name)
    }

    class ListableBeanFactory {
        <<interface>>
        +containsBeanDefinition(name)
        +getBeanDefinitionCount()
        +getBeanDefinitionNames()
        +getBeanNamesForType(type)
        +getBeansOfType(type)
        +getBeansOfTypeAsMap(type)
        +getPrimaryBean(type)
    }

    class ApplicationContext {
        <<interface>>
        +getId()
        +getApplicationName()
        +getDisplayName()
        +getStartupDate()
        +getParent()
        +refresh()
        +close()
        +isActive()
    }

    class BeanDefinitionRegistry {
        <<interface>>
        +registerBeanDefinition(name, bd)
        +removeBeanDefinition(name)
        +getBeanDefinition(name)
        +containsBeanDefinition(name)
        +registerAlias(name, alias)
    }

    class DefaultListableBeanFactory {
        -beanDefinitionMap
        -singletonCache
        -fastBeanLookup
        -lifecycleManager
        -typeConverter
    }

    class AnnotationConfigApplicationContext {
        -beanFactory
        -environment
        -applicationEventMulticaster
    }

    BeanFactory <|-- ListableBeanFactory
    ListableBeanFactory <|-- ApplicationContext
    BeanFactory <|.. DefaultListableBeanFactory
    BeanDefinitionRegistry <|.. DefaultListableBeanFactory
    ApplicationContext <|.. AnnotationConfigApplicationContext
```

## 关键设计模式总结

| 模式 | 应用位置 | 说明 |
|------|----------|------|
| **工厂模式** | BeanFactory / FactoryBean | Bean 的创建和管理 |
| **策略模式** | TypeConverter / Condition | 可替换的算法实现 |
| **观察者模式** | ApplicationEvent / ApplicationListener | 事件发布-订阅 |
| **模板方法模式** | DefaultListableBeanFactory.doCreateBean | 定义 Bean 创建骨架 |
| **组合模式** | SingletonCache | 封装三级缓存复杂性 |
| **单例模式** | 单例 Bean 缓存 | 全局唯一实例 |
| **代理模式** | LazyInvocationHandler | 懒加载动态代理 |
| **建造者模式** | BeanDefinition | Bean 元数据构建 |
| **注册表模式** | BeanDefinitionRegistry | Bean 定义注册和查找 |

## 技术栈

| 依赖 | 用途 |
|------|------|
| objectweb.asm (ASM9) | 字节码扫描，避免类加载 |
| jakarta.annotation | @PostConstruct / @PreDestroy |
| SLF4J | 日志门面 |
