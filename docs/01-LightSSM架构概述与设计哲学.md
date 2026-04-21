# 01 - LightSSM架构概述与设计哲学

## 1. 项目定位与目标

LightSSM是一个教育性质的轻量级SSM框架实现，旨在通过精简的代码帮助开发者理解Spring和MyBatis的核心原理。

### 1.1 设计目标

- **教学导向**：剥离复杂的企业级特性，保留核心机制
- **源码对照**：与Spring 6.x和MyBatis 3.x源码保持结构对应
- **渐进学习**：从基础容器到高级AOP，层层递进

### 1.2 与官方框架的核心差异

| 对比维度 | Spring Framework | LightSSM |
|---------|------------------|----------|
| 代码量 | 约20万行 | 约5000行 |
| 配置方式 | XML/注解/Java Config | 注解+XML混合 |
| BeanFactory层级 | 7层接口继承 | 3层接口 |
| AOP支持 | 完整AspectJ集成 | 基础JDK/CGLIB代理 |
| 事务管理 | 声明式+编程式 | 仅编程式基础 |
| 适用场景 | 企业生产环境 | 学习研究 |

## 2. 整体架构设计

### 2.1 四层架构模型

```
com.lightframework
├── ioc/          # 控制反转容器 - 框架基石
│   ├── annotation/  # 注解定义
│   ├── beans/       # Bean元数据
│   ├── core/        # 核心工厂实现
│   ├── context/     # 应用上下文
│   └── exception/   # 异常体系
│
├── aop/          # 面向切面编程 - 横切关注点
│   ├── annotation/  # 切面注解
│   ├── core/        # 代理工厂
│   ├── interceptor/ # 拦截器实现
│   └── pointcut/    # 切入点表达式
│
├── mvc/          # Web MVC框架 - 请求处理
│   ├── annotation/  # MVC注解
│   ├── core/        # 核心接口
│   ├── handler/     # 处理器实现
│   ├── servlet/     # 前端控制器
│   └── view/        # 视图解析
│
└── orm/          # 对象关系映射 - 数据访问
    ├── annotations/ # SQL注解
    ├── binding/     # Mapper代理
    ├── builder/     # SQL构建
    ├── datasource/  # 数据源
    ├── executor/    # 执行器
    ├── mapping/     # 映射定义
    ├── plugin/      # 插件系统
    ├── reflection/  # 反射工具
    ├── scripting/   # 动态SQL
    ├── session/     # 会话管理
    ├── transaction/ # 事务管理
    └── type/        # 类型处理
```

### 2.2 模块依赖关系

IoC容器是整个框架的基石：
- AOP模块依赖IoC进行切面Bean的管理
- MVC模块依赖IoC进行Controller的实例化
- ORM模块独立运行，但可通过IoC集成

## 3. 核心设计原则

### 3.1 工厂模式贯穿始终

Spring官方源码中，工厂模式无处不在：

```java
// Spring官方：BeanFactory层次结构
BeanFactory
└── ListableBeanFactory
    └── HierarchicalBeanFactory
        └── ConfigurableBeanFactory
            └── AutowireCapableBeanFactory
                └── ConfigurableListableBeanFactory

// LightSSM简化版本
BeanFactory                    # 基础接口
├── ListableBeanFactory        # 列表能力
└── DefaultListableBeanFactory # 核心实现
```

### 3.2 模板方法模式

```java
// Spring官方：AbstractExecutorService
// MyBatis官方：BaseExecutor
// LightSSM实现：BaseExecutor

public abstract class BaseExecutor implements Executor {
    // 模板方法：定义算法骨架
    public int update(MappedStatement ms, Object parameter) {
        // 公共逻辑：事务管理、缓存处理
        return doUpdate(ms, parameter); // 抽象方法：子类实现
    }
    
    // 抽象方法：由子类实现具体逻辑
    protected abstract int doUpdate(MappedStatement ms, Object parameter);
}
```

### 3.3 策略模式应用

类型处理器是策略模式的典型应用：

```java
// 策略接口
public interface TypeHandler<T> {
    void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType);
    T getResult(ResultSet rs, String columnName);
}

// 具体策略
public class StringTypeHandler implements TypeHandler<String> { ... }
public class IntegerTypeHandler implements TypeHandler<Integer> { ... }
```

## 4. 生命周期管理

### 4.1 Bean的完整生命周期

```
1. 实例化（Instantiation）
   ↓
2. 属性填充（Populate）
   ↓
3. Aware接口回调
   ↓
4. BeanPostProcessor前置处理
   ↓
5. 初始化（Initialization）
   ↓
6. BeanPostProcessor后置处理
   ↓
7. 就绪使用（Ready）
   ↓
8. 销毁（Destruction）
```

### 4.2 与Spring官方对比

| 阶段 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 实例化 | 通过Supplier/FactoryMethod/Constructor | 仅支持Constructor |
| Aware接口 | 10+种Aware接口 | 无 |
| 后置处理器 | BeanPostProcessor链 | 基础支持 |
| 销毁回调 | DisposableBean/@PreDestroy | 无 |

## 5. 学习路线建议

### 5.1 渐进式学习路径

```
第1阶段：理解IoC容器（文档02）
  ↓
第2阶段：掌握AOP原理（文档03）
  ↓
第3阶段：学习MVC流程（文档04）
  ↓
第4阶段：深入ORM核心（文档05-06）
  ↓
第5阶段：探索SQL处理（文档07）
  ↓
第6阶段：理解类型系统（文档08）
  ↓
第7阶段：插件与扩展（文档09）
  ↓
第8阶段：综合与深入（文档10）
```

### 5.2 前置知识要求

- Java反射机制
- 动态代理（JDK & CGLIB）
- XML解析（DOM4J）
- JDBC基础
- Servlet规范

## 6. 源码阅读技巧

### 6.1 对照阅读法

推荐同时打开三个源码：
1. LightSSM实现
2. Spring官方源码（GitHub: spring-projects/spring-framework）
3. 官方文档

### 6.2 关键切入点

- Spring IOC：`AbstractAutowireCapableBeanFactory.doCreateBean()`
- Spring AOP：`JdkDynamicAopProxy.invoke()`
- MyBatis：`DefaultSqlSession.selectList()`

### 6.3 调试建议

1. 在关键方法设置断点
2. 观察方法调用栈
3. 注意对象状态变化
4. 记录关键变量值

## 7. 总结

LightSSM通过精简实现保留了Spring和MyBatis的核心机制：

- **IoC**：三级缓存解决循环依赖
- **AOP**：JDK/CGLIB双代理机制
- **MVC**：完整的请求处理链
- **ORM**：SQL映射与执行器模式

这些核心机制正是面试和实际开发中最常涉及的内容。通过本系列文档的学习，你将能够：

1. 理解Spring核心原理
2. 掌握MyBatis运行机制
3. 具备阅读源码的能力
4. 提升面试竞争力

---

**下一步**：[02 - IoC容器核心实现与Spring源码对比](02-IoC容器核心实现与Spring源码对比.md)
