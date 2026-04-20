# 第2阶段：IoC容器核心实现

## 变动内容

### 1. 注解定义（annotation包）
- `@Component` - 组件注解，标记为Spring管理的Bean
- `@Autowired` - 自动注入注解，支持required属性
- `@Scope` - 作用域注解，支持singleton/prototype

### 2. Bean定义（beans包）
- `BeanDefinition` - Bean定义信息类
  - beanName、beanClass、scope属性
  - isSingleton()、isPrototype()判断方法

### 3. 核心接口（core包）
- `BeanFactory` - Bean工厂基础接口
  - getBean()、containsBean()、isSingleton()等方法
- `ListableBeanFactory` - 可列表的Bean工厂接口
  - getBeanDefinitionNames()、getBeansOfType()等方法
- `BeanPostProcessor` - Bean后置处理器接口
  - postProcessBeforeInitialization()
  - postProcessAfterInitialization()
  - getEarlyBeanReference()

### 4. 核心实现（core包）
- `DefaultListableBeanFactory` - 核心Bean工厂实现
  - **三级缓存机制**：
    - singletonObjects（一级缓存：完整Bean）
    - earlySingletonObjects（二级缓存：早期Bean）
    - singletonFactories（三级缓存：Bean工厂）
  - 解决循环依赖的核心逻辑
  - Bean生命周期管理（创建、注入、初始化）

### 5. 应用上下文（context包）
- `ApplicationContext` - 应用上下文接口
- `AnnotationConfigApplicationContext` - 注解配置应用上下文
  - scan()包扫描方法
  - refresh()刷新方法
  - preInstantiateSingletons()预实例化单例
- `ClassPathBeanDefinitionScanner` - 包扫描器
  - 扫描@Component注解类
  - 注册BeanDefinition

### 6. 异常处理（exception包）
- `BeansException` - Bean异常基类
- `BeanCreationException` - Bean创建异常
- `BeanCurrentlyInCreationException` - 循环依赖异常
- `NoSuchBeanDefinitionException` - Bean不存在异常

## 设计说明

### 三级缓存解决循环依赖

**核心原理**：
```
A依赖B，B依赖A的循环依赖场景：

1. 创建A实例，标记为"正在创建"
2. 将A的ObjectFactory暴露到三级缓存（singletonFactories）
3. A注入B，发现B未创建
4. 创建B实例，标记为"正在创建"
5. 将B的ObjectFactory暴露到三级缓存
6. B注入A，从三级缓存获取A的早期引用
7. 将A的早期引用升级到二级缓存（earlySingletonObjects）
8. B完成初始化，放入一级缓存（singletonObjects）
9. A完成初始化，放入一级缓存
```

**关键代码**：
```java
// DefaultListableBeanFactory.java
protected Object getSingleton(String beanName) {
    // 1. 先从一级缓存获取完整Bean
    Object singletonObject = this.singletonObjects.get(beanName);
    
    // 2. 如果正在创建中，从二级缓存获取早期Bean
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        singletonObject = this.earlySingletonObjects.get(beanName);
        
        // 3. 如果二级缓存也没有，从三级缓存获取工厂并创建
        if (singletonObject == null) {
            ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
            if (singletonFactory != null) {
                singletonObject = singletonFactory.getObject();
                // 升级到二级缓存
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
            }
        }
    }
    return singletonObject;
}
```

### Bean生命周期

```
1. instantiateBean() - 实例化Bean
2. addSingletonFactory() - 暴露早期引用（三级缓存）
3. populateBean() - 属性注入（@Autowired）
4. initializeBean() - 初始化Bean
   - applyBeanPostProcessorsBeforeInitialization()
   - invokeInitMethods()
   - applyBeanPostProcessorsAfterInitialization()
5. addSingleton() - 添加到一级缓存
```

## 文件清单
- src/main/java/com/lightframework/ioc/annotation/*.java（3个注解）
- src/main/java/com/lightframework/ioc/beans/BeanDefinition.java
- src/main/java/com/lightframework/ioc/core/*.java（4个核心类）
- src/main/java/com/lightframework/ioc/context/*.java（3个上下文类）
- src/main/java/com/lightframework/ioc/exception/*.java（4个异常类）
- docs/stage2-IoC容器实现.md

## 下一步计划
第3阶段将实现SpringMVC核心功能，包括：
- DispatcherServlet核心分发器
- HandlerMapping处理器映射
- HandlerAdapter处理器适配
- ViewResolver视图解析器