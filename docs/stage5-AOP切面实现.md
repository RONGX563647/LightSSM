# 第5阶段：AOP切面实现

## 变动内容

### 1. AOP注解定义（annotation包）
- `@Aspect` - 切面注解，标记切面类
- `@Before` - 前置通知注解
- `@After` - 后置通知注解
- `@Around` - 环绕通知注解
- `@Pointcut` - 切入点表达式注解

### 2. 核心接口与类（core包）
- `AopProxy` - AOP代理接口
  - getProxy()获取代理对象
- `MethodInterceptor` - 方法拦截器接口
  - invoke()拦截方法执行
- `MethodInvocation` - 方法调用封装
  - proceed()继续执行方法
- `JoinPoint` - 连接点信息
  - target、method、args属性
- `ProceedingJoinPoint` - 可执行的连接点
  - proceed()执行目标方法
- `AdvisedSupport` - 代理配置支持
  - methodInterceptors方法拦截器映射

### 3. JDK动态代理实现（core包）
- `JdkDynamicAopProxy` - JDK动态代理实现
  - 实现InvocationHandler接口
  - invoke()方法拦截逻辑
  - 支持接口代理

### 4. CGLIB代理实现（core包）
- `CglibAopProxy` - CGLIB代理实现
  - 使用Enhancer创建代理
  - MethodInterceptor拦截方法
  - 支持类代理（无接口）

### 5. 代理工厂（core包）
- `ProxyFactory` - 代理工厂
  - createAopProxy()选择代理方式
  - 有接口→JDK动态代理
  - 无接口→CGLIB代理

### 6. 方法拦截器实现（interceptor包）
- `BeforeMethodInterceptor` - 前置通知拦截器
  - 执行@Before方法
  - 然后执行目标方法
- `AfterMethodInterceptor` - 后置通知拦截器
  - 执行目标方法
  - 然后执行@After方法
- `AroundMethodInterceptor` - 环绕通知拦截器
  - 执行@Around方法
  - 通过ProceedingJoinPoint.proceed()控制执行

### 7. 切入点表达式（pointcut包）
- `AspectJExpressionPointcut` - AspectJ表达式切入点
  - execution()表达式解析
  - matches()匹配类和方法

## 设计说明

### JDK/CGLIB双代理机制

**选择策略**：
```java
public class ProxyFactory {
    protected AopProxy createAopProxy() {
        Class<?> targetClass = advised.getTargetClass();
        
        // 有接口 → JDK动态代理
        if (targetClass.getInterfaces().length > 0) {
            return new JdkDynamicAopProxy(advised);
        } 
        // 无接口 → CGLIB代理
        else {
            return new CglibAopProxy(advised);
        }
    }
}
```

**JDK动态代理**：
- 基于接口的代理
- 使用Proxy.newProxyInstance()
- 实现InvocationHandler接口
- 只能代理接口方法

**CGLIB代理**：
- 基于类的代理
- 使用Enhancer创建子类
- 实现MethodInterceptor接口
- 可以代理类方法（包括非接口方法）

### 责任链模式

**拦截器链执行流程**：
```
MethodInvocation.proceed()
    ↓ currentInterceptorIndex++
BeforeMethodInterceptor.invoke()
    ↓ 执行@Before方法
    ↓ invocation.proceed()
MethodInvocation.proceed()
    ↓ currentInterceptorIndex++
AroundMethodInterceptor.invoke()
    ↓ 执行@Around方法
    ↓ pjp.proceed()
MethodInvocation.proceed()
    ↓ currentInterceptorIndex++
AfterMethodInterceptor.invoke()
    ↓ invocation.proceed()
    ↓ 执行@After方法
目标方法执行
```

### AspectJ表达式解析

**execution表达式**：
```
execution(* com.example.service.*.*(..))
```

**解析规则**：
- `*` - 匹配任意返回类型
- `com.example.service.*` - 匹配service包下所有类
- `.*` - 匹配所有方法
- `(..)` - 匹配任意参数

**匹配流程**：
1. convertExpressionToRegex()转换为正则表达式
2. Pattern.matcher()匹配类名和方法名
3. matches()返回匹配结果

## 文件清单
- src/main/java/com/lightframework/aop/annotation/*.java（5个注解）
- src/main/java/com/lightframework/aop/core/*.java（8个核心类）
- src/main/java/com/lightframework/aop/interceptor/*.java（3个拦截器）
- src/main/java/com/lightframework/aop/pointcut/*.java（1个切入点类）
- docs/stage5-AOP切面实现.md

## 下一步计划
第6阶段将编写完整文档和README，包括：
- 项目README.md
- 开发文档.md
- Git提交总结