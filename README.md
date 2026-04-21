# Light Framework - 轻量级 SSM 框架

> 📖 **在线文档**: [https://rongx563647.github.io/LightSSM/](https://rongx563647.github.io/LightSSM/)

## 项目简介

Light Framework 是一个轻量级 SSM 框架，完整实现了 IoC 容器、SpringMVC、ORM 和 AOP 核心功能，适合用于学习 Spring 框架原理和简历展示。

### 核心价值

- 💡 **深度学习**: 通过手写精简版 Spring + SpringMVC + MyBatis，深入理解框架核心设计思想
- 🎯 **面试加分**: 最加分的项目经历，让你在众多候选人中脱颖而出
- 📚 **理论实践结合**: 别人只会背原生 Spring 理论，你是原生 Spring 原理 + 自己手写精简框架对照理解
- 🔥 **考点锚定**: 所有面试考点锚定你的项目，不再空洞，面试官深挖你完全接得住

## 核心特性

### 1. IoC/DI 容器
- ✅ 支持 `@Component`、`@Autowired` 注解扫描与依赖注入
- ✅ **基于三级缓存解决循环依赖问题**
- ✅ 支持 `@Scope` 注解配置单例/原型模式

### 2. SpringMVC 核心
- ✅ 实现 `DispatcherServlet`、`HandlerMapping`、`HandlerAdapter`
- ✅ 支持 `@RequestMapping`、`@ResponseBody` 开发 RESTful 接口
- ✅ 支持 `@RequestParam`、`@PathVariable` 参数绑定
- ✅ 实现 ViewResolver 视图解析器

### 3. ORM 模块
- ✅ 支持 XML SQL 映射配置
- ✅ 支持动态 SQL 拼接（OGNL 表达式）
- ✅ 参数自动绑定与 ResultSet 映射
- ✅ 支持插件拦截器机制

### 4. AOP 切面
- ✅ **基于 JDK/CGLIB 双代理实现**
- ✅ 支持 `@Before`、`@After`、`@Around` 通知织入
- ✅ 支持 AspectJ 表达式切入点

## 技术架构

```
com.lightframework
├── ioc/                    # IoC 容器模块
│   ├── annotation/         # 注解定义
│   ├── beans/              # Bean 定义
│   ├── core/               # 核心接口与实现
│   ├── context/            # 应用上下文
│   └── exception/          # 异常处理
├── mvc/                    # SpringMVC 模块
│   ├── annotation/         # MVC 注解
│   ├── core/               # 核心接口
│   ├── handler/            # 处理器映射与适配
│   ├── servlet/            # DispatcherServlet
│   └── view/               # 视图解析
├── orm/                    # ORM 模块
│   ├── session/            # 会话管理
│   ├── executor/           # SQL 执行器
│   ├── binding/            # Mapper 代理
│   ├── mapping/            # SQL 映射
│   ├── builder/            # SQL 构建器
│   ├── parsing/            # 表达式解析
│   ├── plugin/             # 插件拦截器
│   └── type/               # 类型处理
└── aop/                    # AOP 模块
    ├── annotation/         # AOP 注解
    ├── core/               # 核心接口
    ├── interceptor/        # 方法拦截器
    └── pointcut/           # 切入点表达式
```

## 快速开始

### 1. IoC 容器使用

```java
// 创建应用上下文
ApplicationContext context = new AnnotationConfigApplicationContext("com.example");

// 获取 Bean
UserService userService = context.getBean(UserService.class);
```

### 2. SpringMVC 使用

```java
@Controller
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @RequestMapping("/list")
    @ResponseBody
    public List<User> list() {
        return userService.findAll();
    }
}
```

### 3. ORM 使用

```xml
<mapper namespace="UserMapper">
    <select id="selectUserById" resultType="com.example.User">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

### 4. AOP 使用

```java
@Aspect
@Component
public class LogAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void before(JoinPoint joinPoint) {
        System.out.println("Before: " + joinPoint.getMethodName());
    }
    
    @Around("execution(* com.example.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("Around before");
        Object result = pjp.proceed();
        System.out.println("Around after");
        return result;
    }
}
```

## 详细技术实现

### 一、IoC/DI 容器模块

#### 1.1 核心类说明

**DefaultListableBeanFactory** - Bean 工厂核心实现
- 基于三级缓存解决循环依赖问题
  - `singletonObjects`: 一级缓存，存放完全初始化完成的单例 Bean
  - `earlySingletonObjects`: 二级缓存，存放早期暴露的 Bean 引用
  - `singletonFactories`: 三级缓存，存放 ObjectFactory，用于生成早期引用
- 使用 `ConcurrentHashMap` 保证线程安全
- 支持 Bean 的生命周期管理：实例化 → 属性填充 → 初始化

**AnnotationConfigApplicationContext** - 注解配置上下文
- 实现 `ApplicationContext` 接口，提供完整的 IoC 容器功能
- 支持包扫描：通过 `ClassPathBeanDefinitionScanner` 扫描指定包下的组件
- 支持注解注册：`@Component`、`@Autowired`、`@Scope` 等
- 容器刷新机制：`refresh()` 方法完成 Bean 定义注册、后处理器注册、单例预实例化

#### 1.2 依赖注入实现

**属性注入流程** (`populateBean` 方法):
1. 遍历 Bean 类的所有字段
2. 检测 `@Autowired` 注解
3. 根据字段类型调用 `resolveDependency()` 解析依赖
4. 通过反射设置字段值：`field.setAccessible(true)` → `field.set(bean, dependency)`

**循环依赖解决方案**:
```java
// 三级缓存工作机制
protected Object getSingleton(String beanName) {
    // 1. 尝试从一级缓存获取
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        // 2. 尝试从二级缓存获取早期引用
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null) {
            // 3. 从三级缓存获取 ObjectFactory 并创建早期引用
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

#### 1.3 设计模式应用

- **工厂模式**: `DefaultListableBeanFactory` 作为 Bean 工厂
- **单例模式**: 单例 Bean 存储在 `singletonObjects` 缓存中
- **策略模式**: `BeanPostProcessor` 提供 Bean 初始化前后的扩展点
- **模板方法模式**: `doGetBean()` 定义 Bean 获取的模板流程

---

### 二、SpringMVC 模块

#### 2.1 核心组件

**DispatcherServlet** - 前端控制器
- 继承 `HttpServlet`，作为 MVC 的请求入口
- 初始化流程:
  - `initApplicationContext()`: 创建 IoC 容器
  - `initHandlerMappings()`: 初始化处理器映射
  - `initHandlerAdapters()`: 初始化处理器适配器
  - `initViewResolvers()`: 初始化视图解析器
- 请求处理流程 (`doDispatch` 方法):
  1. 通过 `HandlerMapping` 获取 `HandlerExecutionChain`
  2. 通过 `HandlerAdapter` 执行处理器方法
  3. 应用拦截器链的 `preHandle` 和 `postHandle`
  4. 通过 `ViewResolver` 解析视图并渲染响应

**RequestMappingHandlerMapping** - 请求映射处理器
- 扫描所有带 `@RequestMapping` 注解的 Controller
- 建立 URL 路径到 `HandlerMethod` 的映射关系
- 使用 `ConcurrentHashMap` 存储映射缓存
- 支持拦截器链的动态添加

**RequestMappingHandlerAdapter** - 请求适配器
- 支持 `@RequestParam` 参数绑定
- 支持 `@PathVariable` 路径变量绑定
- 支持 `@ResponseBody` 响应体注解
- 自动处理请求参数类型转换

#### 2.2 请求处理流程

```
用户请求 → DispatcherServlet
         ↓
    getHandler(request) → HandlerMapping
         ↓
    getHandlerAdapter(handler) → HandlerAdapter
         ↓
    interceptor.preHandle()
         ↓
    adapter.handle() → 执行 Controller 方法
         ↓
    interceptor.postHandle()
         ↓
    processDispatchResult() → 渲染视图
         ↓
    interceptor.afterCompletion()
```

#### 2.3 设计模式应用

- **前端控制器模式**: `DispatcherServlet` 统一处理所有请求
- **策略模式**: `HandlerAdapter` 支持多种处理器类型
- **责任链模式**: `HandlerInterceptor` 拦截器链
- **适配器模式**: `HandlerAdapter` 适配不同类型的处理器

---

### 三、ORM 模块

#### 3.1 核心架构

**SqlSession** - 数据库会话接口
- 提供 CRUD 操作方法：`selectOne()`, `selectList()`, `insert()`, `update()`, `delete()`
- 事务控制：`commit()`, `rollback()`
- Mapper 代理：`getMapper(Class<T> type)`
- 继承 `Closeable`，支持 try-with-resources

**BaseExecutor** - 执行器基类
- 模板方法模式定义执行流程
- 抽象方法：`doUpdate()`, `doQuery()`
- 事务管理：委托给 `Transaction` 对象
- 缓存管理：一级缓存实现基础
- 资源清理：自动关闭 `Statement` 和 `Connection`

**MappedStatement** - SQL 映射语句
- 存储 SQL 执行所需的所有元数据
- 属性包括：`id`, `sqlCommandType`, `sqlSource`, `resultType`, `resultMaps`
- 支持主键回填：`keyGenerator`, `keyProperties`, `keyColumns`
- 使用建造者模式构建：`MappedStatement.Builder`

#### 3.2 SQL 解析与构建

**SqlSourceBuilder** - SQL 源构建器
- 解析 `#{}` 占位符，替换为 `?`
- 构建 `ParameterMapping` 列表，记录参数类型和属性
- 生成 `StaticSqlSource` 对象

**GenericTokenParser** - 通用令牌解析器
- 支持自定义开始和结束标记（如 `#{` 和 `}`）
- 委托 `TokenHandler` 处理匹配的内容
- 用于解析动态 SQL 中的表达式

**ParameterExpression** - 参数表达式解析
- 解析 `#{property,jdbcType=VARCHAR}` 格式
- 提取 `property`、`jdbcType` 等属性
- 存储为 `Map<String, String>`

#### 3.3 Mapper 代理机制

**MapperProxy** - Mapper 代理类
- 实现 `InvocationHandler` 接口（JDK 动态代理）
- 拦截 Mapper 接口方法调用
- 根据方法签名查找对应的 `MappedStatement`
- 委托 `SqlSession` 执行 SQL

**MapperRegistry** - Mapper 注册器
- 维护 Mapper 接口与代理工厂的映射关系
- 使用 `HashMap<Class<?>, MapperProxyFactory<?>>` 存储
- 提供 `getMapper()` 方法创建代理实例

#### 3.4 插件拦截器机制

**InterceptorChain** - 拦截器链
- 责任链模式管理多个 `Interceptor`
- 提供 `pluginAll()` 方法包装目标对象
- 按顺序执行所有拦截器

**Plugin** - 插件包装器
- 实现 `InvocationHandler` 接口
- 根据 `@Intercepts` 注解的 `@Signature` 判断是否拦截
- 支持方法级别的精确拦截

**Invocation** - 调用封装
- 封装反射调用的所有信息：`target`, `method`, `args`
- 提供 `proceed()` 方法执行调用
- 统一异常处理

#### 3.5 设计模式应用

- **工厂模式**: `SqlSessionFactory` 创建 `SqlSession`
- **代理模式**: `MapperProxy` 代理 Mapper 接口
- **模板方法模式**: `BaseExecutor` 定义 SQL 执行模板
- **建造者模式**: `MappedStatement.Builder` 构建复杂对象
- **策略模式**: `TypeHandler` 处理不同类型转换
- **责任链模式**: `InterceptorChain` 执行拦截器链
- **单例模式**: `Configuration` 配置对象全局唯一

---

### 四、AOP 模块

#### 4.1 双代理实现

**ProxyFactory** - 代理工厂
- 根据目标类是否有接口自动选择代理策略
- 有接口 → 使用 JDK 动态代理
- 无接口 → 使用 CGLIB 代理

**JdkDynamicAopProxy** - JDK 动态代理
- 实现 `InvocationHandler` 接口
- 基于 Java 反射机制创建代理
- 仅能代理接口，不能代理类
- 性能较好，无需额外依赖

**CglibAopProxy** - CGLIB 代理
- 使用 `Enhancer` 创建目标类的子类
- 通过 `MethodInterceptor` 拦截方法调用
- 可以代理具体类，无需接口
- 性能略低于 JDK 代理，需要 CGLIB 依赖

#### 4.2 通知类型实现

**AroundMethodInterceptor** - 环绕通知
- 最强大的通知类型
- 可以控制目标方法的执行时机
- 支持修改返回值
- 可以抛出异常阻止执行

**BeforeMethodInterceptor** - 前置通知
- 在目标方法执行前执行
- 无法阻止目标方法执行
- 无法修改返回值

**AfterMethodInterceptor** - 后置通知
- 在目标方法执行后执行
- 无法修改返回值
- 类似于 `finally` 块

#### 4.3 方法调用链

**MethodInvocation** - 方法调用封装
- 封装目标对象、方法、参数、代理对象
- 维护拦截器链和执行索引
- `proceed()` 方法递归执行拦截器
- 执行完所有拦截器后调用目标方法

```java
public Object proceed() throws Throwable {
    if (this.interceptorIndex < interceptors.size()) {
        // 递归执行下一个拦截器
        MethodInterceptor interceptor = interceptors.get(this.interceptorIndex++);
        return interceptor.invoke(this);
    } else {
        // 所有拦截器执行完毕，调用目标方法
        return method.invoke(target, args);
    }
}
```

#### 4.4 设计模式应用

- **代理模式**: JDK 和 CGLIB 双代理实现
- **责任链模式**: `MethodInvocation` 执行拦截器链
- **策略模式**: 根据目标类自动选择代理策略
- **工厂模式**: `ProxyFactory` 创建代理对象

---

### 五、关键技术点总结

#### 5.1 并发安全
- 所有缓存使用 `ConcurrentHashMap`
- Bean 创建使用 `ConcurrentHashMap` 的原子操作
- 线程局部变量管理事务和连接

#### 5.2 反射优化
- 缓存 `Method` 和 `Field` 对象避免重复查找
- 使用 `setAccessible(true)` 提升反射性能
- 延迟加载减少启动时间

#### 5.3 缓存机制
- IoC 三级缓存解决循环依赖
- ORM 一级缓存减少数据库查询
- HandlerMapping 缓存提升请求处理速度

#### 5.4 扩展性设计
- BeanPostProcessor 扩展 Bean 生命周期
- Interceptor 拦截器链支持功能增强
- TypeHandler 支持自定义类型转换
- Plugin 插件机制支持 SQL 拦截修改

---

## 与 Spring 官方源码对比

### IoC 容器对比

| 功能模块 | Spring 官方实现 | LightSSM 实现 | 核心差异 |
|----------|----------------|---------------|----------|
| **BeanDefinition** | `BeanDefinition` 接口 + `RootBeanDefinition` 等实现类 | 简化的 `BeanDefinition` 类 | Spring 支持更多属性和继承体系，LightSSM 只保留核心字段 |
| **BeanFactory** | `DefaultListableBeanFactory` (1000+ 行) | `DefaultListableBeanFactory` (370+ 行) | Spring 支持更多特性（FactoryBean、Aware 接口等），LightSSM 精简为核心逻辑 |
| **三级缓存** | `singletonObjects`、`earlySingletonObjects`、`singletonFactories` | 完全一致的三级缓存结构 | **核心算法一致**，LightSSM 完整复现了循环依赖解决方案 |
| **依赖注入** | `AutowiredAnnotationBeanPostProcessor` | `populateBean()` 方法直接实现 | Spring 使用 BPP 扩展，LightSSM 直接在内核中实现 |
| **包扫描** | `ClassPathBeanDefinitionScanner` | `ClassPathBeanDefinitionScanner` | **实现原理一致**，都是扫描 classpath 并注册 BeanDefinition |
| **ApplicationContext** | `AnnotationConfigApplicationContext` | `AnnotationConfigApplicationContext` | Spring 支持更多生命周期和事件，LightSSM 保留核心刷新流程 |

### SpringMVC 对比

| 功能模块 | Spring 官方实现 | LightSSM 实现 | 核心差异 |
|----------|----------------|---------------|----------|
| **DispatcherServlet** | `FrameworkServlet` → `DispatcherServlet` 继承体系 | 直接继承 `HttpServlet` | Spring 支持 WebApplicationContext 集成，LightSSM 简化为独立实现 |
| **HandlerMapping** | `RequestMappingHandlerMapping` (基于 `RequestMappingInfo`) | `RequestMappingHandlerMapping` (基于 URL 字符串) | Spring 支持更复杂的路径匹配（Ant 风格、正则），LightSSM 使用精确匹配 |
| **HandlerAdapter** | 多个实现类（`HttpRequestHandlerAdapter`、`SimpleControllerHandlerAdapter` 等） | `RequestMappingHandlerAdapter` 单一实现 | Spring 支持多种处理器类型，LightSSM 只支持注解 Controller |
| **ViewResolver** | `InternalResourceViewResolver`、`ThymeleafViewResolver` 等 | `InternalResourceViewResolver` | Spring 支持多种视图技术，LightSSM 只支持 JSP |
| **参数解析** | `HandlerMethodArgumentResolver` 链 | 直接反射 + 注解解析 | Spring 支持 20+ 种参数类型，LightSSM 支持基础的 `@RequestParam`、`@PathVariable` |

### ORM 对比（MyBatis）

| 功能模块 | MyBatis 官方实现 | LightSSM 实现 | 核心差异 |
|----------|-----------------|---------------|----------|
| **SqlSession** | `DefaultSqlSession` (400+ 行) | `DefaultSqlSession` (简化版) | MyBatis 支持更多重载方法和缓存，LightSSM 保留核心 CRUD |
| **Executor** | `SimpleExecutor`、`ReuseExecutor`、`BatchExecutor` | `BaseExecutor` + `SimpleExecutor` | MyBatis 支持多种执行策略，LightSSM 只实现基础版本 |
| **MappedStatement** | 复杂的 `MappedStatement` (30+ 字段) | 简化的 `MappedStatement` (10+ 字段) | MyBatis 支持缓存、flush、超时等，LightSSM 只保留 SQL 执行必需字段 |
| **SQL 解析** | `SqlSourceBuilder` + `GenericTokenParser` | 完全一致的实现 | **核心算法一致**，包括 `#{}` 和 `${}` 的处理 |
| **Mapper 代理** | `MapperProxy` + `MapperMethod` | `MapperProxy` + `MapperMethod` | **实现原理一致**，都是 JDK 动态代理 + 方法签名映射 |
| **插件机制** | `InterceptorChain` + `Plugin` | 完全一致的责任链实现 | **核心算法一致**，支持 `@Intercepts` 注解 |

### AOP 对比

| 功能模块 | Spring 官方实现 | LightSSM 实现 | 核心差异 |
|----------|----------------|---------------|----------|
| **代理工厂** | `ProxyFactory` (支持多种配置) | `ProxyFactory` (简化版) | Spring 支持 `Advised` 配置，LightSSM 直接基于目标类创建 |
| **JDK 代理** | `JdkDynamicAopProxy` | `JdkDynamicAopProxy` | **实现原理一致**，都是 `InvocationHandler` |
| **CGLIB 代理** | `ObjenesisCglibAopProxy` | `CglibAopProxy` | Spring 使用 Objenesis 优化对象创建，LightSSM 使用原生 Enhancer |
| **通知类型** | `MethodBeforeAdvice`、`AfterReturningAdvice`、`ThrowsAdvice` | `BeforeMethodInterceptor`、`AfterMethodInterceptor`、`AroundMethodInterceptor` | **功能一致**，接口命名不同 |
| **切入点表达式** | AspectJ 表达式解析器 | AspectJ 表达式解析器 | **使用相同的 AspectJ 库** |
| **拦截器链** | `ReflectiveMethodInvocation` | `MethodInvocation` | **核心算法一致**，递归执行 `proceed()` |

### 核心代码对比

#### Spring 官方三级缓存实现

```java
// Spring DefaultSingletonBeanRegistry.java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null && allowEarlyReference) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

#### LightSSM 三级缓存实现

```java
// LightSSM DefaultListableBeanFactory.java
private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);

protected Object getSingleton(String beanName) {
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        singletonObject = this.earlySingletonObjects.get(beanName);
        if (singletonObject == null) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

**对比结论**: LightSSM 的三级缓存实现与 Spring 官方**核心算法完全一致**，只是在 Spring 的基础上做了适当简化（去掉了 `allowEarlyReference` 参数），完整保留了循环依赖解决机制。

---

## 面试考点

基于 LightSSM 项目的核心面试问题：

### 1. IoC 相关

**Q: IOC、DI 是什么？你的 LightSSM 如何实现？**

**参考答案**:
- IOC（控制反转）是将对象创建权交给容器，DI（依赖注入）是容器自动注入依赖
- LightSSM 通过 `@Component` 扫描注册 Bean，通过 `@Autowired` 实现字段注入
- 核心实现：包扫描 → 生成 BeanDefinition → 存入 `beanDefinitionMap` → 实例化 → 属性填充

**Q: Spring 三级缓存机制是什么？你的 LightSSM 如何实现的？**

**参考答案**:
- 一级缓存 `singletonObjects`: 存放完全初始化的单例 Bean
- 二级缓存 `earlySingletonObjects`: 存放早期暴露的 Bean 引用（未完全初始化）
- 三级缓存 `singletonFactories`: 存放 ObjectFactory，用于生成早期引用
- LightSSM 完整复现了 Spring 的三级缓存机制，代码结构和核心算法与 Spring 官方一致
- 解决循环依赖流程：A 依赖 B，B 依赖 A → A 实例化后暴露早期引用到三级缓存 → B 获取 A 的早期引用 → B 完成初始化 → A 获取 B 完成初始化

### 2. SpringMVC 相关

**Q: SpringMVC 流程？你的 DispatcherServlet 怎么写的？**

**参考答案**:
1. 用户请求 → DispatcherServlet
2. DispatcherServlet → HandlerMapping 获取 HandlerExecutionChain
3. DispatcherServlet → HandlerAdapter 获取适配器
4. 执行拦截器链的 preHandle
5. HandlerAdapter 反射执行 Controller 方法
6. 执行拦截器链的 postHandle
7. ViewResolver 解析视图并渲染
8. 执行拦截器链的 afterCompletion
- LightSSM 的 DispatcherServlet 继承 HttpServlet，在 init() 方法中初始化 IoC 容器、HandlerMapping、HandlerAdapter、ViewResolver

### 3. ORM 相关

**Q: MyBatis 如何整合进 Spring 容器？**

**参考答案**:
- Spring 通过 `SqlSessionFactoryBean` 创建 SqlSessionFactory 并注册为 Bean
- 通过 `MapperScannerConfigurer` 扫描 Mapper 接口并注册为代理 Bean
- LightSSM 实现：在 IoC 容器刷新时创建 SqlSessionFactory → 通过 `getMapper()` 创建 Mapper 代理 → 将代理注入到 Service 的 `@Autowired` 字段

### 4. AOP 相关

**Q: AOP 实现原理？你的 LightSSM 支持哪些通知类型？**

**参考答案**:
- AOP 基于动态代理：有接口用 JDK 代理，无接口用 CGLIB 代理
- JDK 代理：`Proxy.newProxyInstance()` + `InvocationHandler`
- CGLIB 代理：`Enhancer` 创建子类 + `MethodInterceptor`
- LightSSM 支持三种通知类型：
  - `@Before`: 前置通知，在目标方法前执行
  - `@After`: 后置通知，在目标方法后执行
  - `@Around`: 环绕通知，可以控制目标方法执行，修改返回值
- 拦截器链：使用 `MethodInvocation.proceed()` 递归执行所有拦截器

---

## 设计模式

| 设计模式 | 应用位置 |
|----------|----------|
| 工厂模式 | SqlSessionFactory、BeanFactory、ProxyFactory |
| 代理模式 | MapperProxy、JdkDynamicAopProxy、CglibAopProxy |
| 单例模式 | 单例 Bean、Configuration 配置对象 |
| 模板方法 | BaseExecutor、DispatcherServlet |
| 建造者模式 | MappedStatement.Builder、ParameterMapping.Builder |
| 策略模式 | TypeHandler、HandlerAdapter、代理选择策略 |
| 责任链模式 | InterceptorChain、MethodInterceptor、HandlerInterceptor |
| 适配器模式 | HandlerAdapter 适配不同处理器 |
| 前端控制器 | DispatcherServlet 统一请求分发 |

---

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 5.7+

## 构建与测试

```bash
# 编译
mvn clean compile

# 测试
mvn test

# 打包
mvn clean package
```

## Git 提交记录

本项目按照开发阶段分 6 次提交，每次提交都有详细的文档说明：

1. **stage1**: 项目结构初始化（3 个文件，205 行）
2. **stage2**: IoC 容器核心实现（16 个文件，940 行）
3. **stage3**: SpringMVC 核心实现（20 个文件，976 行）
4. **stage4**: ORM 模块完善（159 个文件，16,584 行）
5. **stage5**: AOP 切面实现（19 个文件，525 行）
6. **stage6**: 文档和 README（本提交）

## License

MIT License
