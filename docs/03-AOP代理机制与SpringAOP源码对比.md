# 03 - AOP代理机制与Spring AOP源码对比

## 1. AOP概述

AOP（Aspect-Oriented Programming，面向切面编程）是OOP的补充，用于将横切关注点（如日志、事务、安全）与业务逻辑分离。

### 1.1 核心概念

```
切面（Aspect）：横切关注点的模块化，如日志切面
通知（Advice）：在特定连接点执行的动作，如@Before、@After
切入点（Pointcut）：匹配连接点的表达式
连接点（JoinPoint）：程序执行过程中的某个点，如方法调用
目标对象（Target）：被代理的对象
代理对象（Proxy）：织入通知后生成的对象
织入（Weaving）：将通知应用到目标对象的过程
```

### 1.2 Spring AOP vs AspectJ

| 对比项 | Spring AOP | AspectJ |
|-------|-----------|---------|
| 实现方式 | 动态代理（运行时） | 字节码织入（编译/加载时） |
| 代理对象 | 仅支持方法级别 | 支持字段、构造器等 |
| 性能 | 有代理开销 | 无运行时开销 |
| 学习成本 | 低 | 高 |
| 适用场景 | 企业应用开发 | 需要精细控制的场景 |

**LightSSM采用Spring AOP方案**：基于JDK动态代理和CGLIB代理。

## 2. 代理模式选择策略

### 2.1 Spring官方的代理选择

Spring官方使用`AopProxyFactory`来决定使用哪种代理：

```java
// Spring官方源码 DefaultAopProxyFactory
public AopProxy createAopProxy(AdvisedSupport config) {
    if (config.isOptimize() || config.isProxyTargetClass() 
        || hasNoUserSuppliedProxyInterfaces(config)) {
        Class<?> targetClass = config.getTargetClass();
        if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
            return new JdkDynamicAopProxy(config);
        }
        return new ObjenesisCglibAopProxy(config);
    } else {
        return new JdkDynamicAopProxy(config);
    }
}
```

### 2.2 LightSSM的代理选择

**源码位置**：[ProxyFactory.java:L21-29](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/core/ProxyFactory.java#L21-L29)

```java
protected AopProxy createAopProxy() {
    Class<?> targetClass = advised.getTargetClass();
    
    // 如果目标类实现了接口，使用JDK动态代理
    if (targetClass.getInterfaces().length > 0) {
        return new JdkDynamicAopProxy(advised);
    } else {
        // 否则使用CGLIB代理
        return new CglibAopProxy(advised);
    }
}
```

**对比分析**：

| 维度 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 判断条件 | 多个条件组合 | 仅看是否实现接口 |
| 配置覆盖 | proxyTargetClass | 不支持配置 |
| 优化选项 | Optimize标志 | 不支持 |
| 策略灵活性 | 可通过AopProxyFactory扩展 | 硬编码在ProxyFactory中 |

### 2.3 JDK vs CGLIB 代理对比

| 对比项 | JDK动态代理 | CGLIB代理 |
|-------|-----------|----------|
| 代理对象 | 接口 | 类的子类 |
| 实现方式 | java.lang.reflect.Proxy | net.sf.cglib.proxy.Enhancer |
| 性能 | 方法调用较慢 | 方法调用较快 |
| 创建速度 | 快 | 慢（需生成字节码） |
| final方法 | 不适用 | 不可代理final方法 |
| 内存占用 | 低 | 高 |

**Spring官方的变化**：从Spring Boot 2.x开始，默认使用CGLIB代理（proxyTargetClass=true）。

## 3. JDK动态代理实现

### 3.1 核心接口

**AopProxy接口**：
```java
public interface AopProxy {
    Object getProxy();
    Object getProxy(ClassLoader classLoader);
}
```

### 3.2 JdkDynamicAopProxy实现

**源码位置**：[JdkDynamicAopProxy.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/core/JdkDynamicAopProxy.java)

```java
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler {
    
    private final AdvisedSupport advised;
    
    public JdkDynamicAopProxy(AdvisedSupport advised) {
        this.advised = advised;
    }
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Class<?>[] proxiedInterfaces = advised.getTargetClass().getInterfaces();
        if (proxiedInterfaces.length == 0) {
            proxiedInterfaces = new Class<?>[]{advised.getTargetClass()};
        }
        
        return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object target = advised.getTarget();
        
        // 获取该方法的所有拦截器
        List<MethodInterceptor> interceptors = advised.getInterceptors(method);
        
        // 如果没有拦截器，直接调用目标方法
        if (interceptors == null || interceptors.isEmpty()) {
            method.setAccessible(true);
            return method.invoke(target, args);
        }
        
        // 创建方法调用对象，执行拦截器链
        MethodInvocation invocation = new MethodInvocation(
            target, method, args, proxy, interceptors);
        return invocation.proceed();
    }
}
```

### 3.3 与Spring官方对比

**Spring官方的JdkDynamicAopProxy.invoke()核心逻辑**：

```java
// Spring官方源码简化版本
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object oldProxy = null;
    boolean setProxyContext = false;
    Object target = null;
    TargetSource targetSource = this.advised.getTargetSource();
    
    try {
        // 1. 处理equals/hashCode方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        // 2. 处理Advised接口方法
        if (this.advised.exposeProxy) {
            oldProxy = AopContext.setCurrentProxy(proxy);
            setProxyContext = true;
        }
        
        target = targetSource.getTarget();
        Class<?> targetClass = target.getClass();
        
        // 3. 获取拦截器链
        List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);
        
        Object retVal;
        if (chain.isEmpty() && Modifier.isPublic(method.getModifiers())) {
            // 4. 没有拦截器，直接调用
            retVal = method.invoke(target, args);
        } else {
            // 5. 创建MethodInvocation并执行
            retVal = new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain).proceed();
        }
        
        return retVal;
    } finally {
        // 6. 清理上下文
        if (setProxyContext && oldProxy != null) {
            AopContext.setCurrentProxy(oldProxy);
        }
    }
}
```

**关键差异**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| Object方法处理 | 特殊处理equals/hashCode | 不处理 |
| 代理上下文暴露 | AopContext.setCurrentProxy | 不支持 |
| 拦截器链获取 | 包含动态匹配 | 静态匹配 |
| MethodInvocation | ReflectiveMethodInvocation | 自定义MethodInvocation |

## 4. CGLIB代理实现

### 4.1 CglibAopProxy实现

**源码位置**：[CglibAopProxy.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/core/CglibAopProxy.java)

```java
public class CglibAopProxy implements AopProxy {
    
    private final AdvisedSupport advised;
    
    @Override
    public Object getProxy(ClassLoader classLoader) {
        Object target = advised.getTarget();
        Class<?> targetClass = advised.getTargetClass();
        
        net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
        enhancer.setClassLoader(classLoader);
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(new CglibMethodInterceptor(advised));
        
        return enhancer.create();
    }
    
    private static class CglibMethodInterceptor implements net.sf.cglib.proxy.MethodInterceptor {
        private final AdvisedSupport advised;
        
        @Override
        public Object intercept(Object obj, Method method, Object[] args, 
            net.sf.cglib.proxy.MethodProxy proxy) throws Throwable {
            
            Object target = advised.getTarget();
            List<MethodInterceptor> interceptors = advised.getInterceptors(method);
            
            if (interceptors == null || interceptors.isEmpty()) {
                return proxy.invoke(target, args);
            }
            
            MethodInvocation invocation = new MethodInvocation(
                target, method, args, obj, interceptors);
            return invocation.proceed();
        }
    }
}
```

### 4.2 CGLIB代理原理

CGLIB通过生成目标类的子类来实现代理：

```
UserService（目标类）
    ↓
Enhancer生成
    ↓
UserService$$EnhancerByCGLIB$$xxx（代理类，继承UserService）
    ↓
方法被重写，加入拦截器逻辑
```

### 4.3 与Spring官方对比

**Spring官方的ObjenesisCglibAopProxy**：

```java
// Spring官方使用Objenesis来创建代理实例
// 避免调用目标类的构造方法
class CglibAopProxy implements AopProxy {
    protected Object createProxyClassAndInstance(Enhancer enhancer, Callback[] callbacks) {
        Class<?> proxyClass = enhancer.createClass();
        Object proxyInstance = objenesis.newInstance(proxyClass);
        ((Factory) proxyInstance).setCallbacks(callbacks);
        return proxyInstance;
    }
}
```

**关键差异**：

| 维度 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 实例创建 | Objenesis（不调用构造方法） | enhancer.create()（调用构造方法） |
| Callback设置 | setCallbacks批量设置 | setCallback单个设置 |
| 回调类型 | MultipleCallback | 单一MethodInterceptor |
| 性能优化 | 缓存Proxy类 | 每次都创建 |

## 5. 拦截器链机制

### 5.1 MethodInterceptor接口

**LightSSM定义**：
```java
public interface MethodInterceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

**Spring官方定义**（org.aopalliance.intercept.MethodInterceptor）：
```java
public interface MethodInterceptor extends Interceptor {
    Object invoke(MethodInvocation invocation) throws Throwable;
}
```

**接口完全一致**：LightSSM直接使用了与Spring官方相同的接口定义。

### 5.2 MethodInvocation实现

**源码位置**：[MethodInvocation.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/core/MethodInvocation.java)

```java
public class MethodInvocation {
    private Object target;                     // 目标对象
    private Method method;                     // 目标方法
    private Object[] args;                     // 方法参数
    private Object proxy;                      // 代理对象
    private List<MethodInterceptor> interceptors; // 拦截器列表
    private int currentInterceptorIndex = -1;  // 当前拦截器索引
    
    public Object proceed() throws Throwable {
        // 移动到下一个拦截器
        currentInterceptorIndex++;
        
        // 如果还有拦截器，执行拦截器
        if (currentInterceptorIndex < interceptors.size()) {
            MethodInterceptor interceptor = interceptors.get(currentInterceptorIndex);
            return interceptor.invoke(this);
        }
        
        // 所有拦截器执行完毕，调用目标方法
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
```

### 5.3 责任链模式执行流程

```
调用代理方法
    ↓
MethodInvocation.proceed()
    ↓
index = 0，执行Interceptor1.invoke(this)
    ↓
Interceptor1执行前置逻辑
    ↓
调用invocation.proceed()（递归）
    ↓
index = 1，执行Interceptor2.invoke(this)
    ↓
Interceptor2执行前置逻辑
    ↓
调用invocation.proceed()（递归）
    ↓
index = 2，没有更多拦截器
    ↓
执行目标方法 method.invoke(target, args)
    ↓
返回到Interceptor2
    ↓
Interceptor2执行后置逻辑
    ↓
返回到Interceptor1
    ↓
Interceptor1执行后置逻辑
    ↓
返回给调用者
```

### 5.4 与Spring官方对比

**Spring官方的ReflectiveMethodInvocation.proceed()**：

```java
public Object proceed() throws Throwable {
    // 如果所有拦截器都执行完了
    if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
        return invokeJoinpoint(); // 调用目标方法
    }
    
    // 获取下一个拦截器或动态匹配器
    Object interceptorOrInterceptionAdvice = 
        this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    
    if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
        // 动态匹配：运行时检查是否匹配
        InterceptorAndDynamicMethodMatcher dm = 
            (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
        if (!dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
            return proceed(); // 不匹配，跳过
        }
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    } else {
        // 静态匹配：编译时确定
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}
```

**关键差异**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 动态匹配 | 支持运行时动态匹配 | 仅静态匹配 |
| 拦截器类型 | 支持多种拦截器类型 | 仅MethodInterceptor |
| 目标方法调用 | invokeJoinpoint() | method.invoke()直接调用 |
| 递归逻辑 | 相同（递归调用proceed） | 相同 |

## 6. 通知类型实现

### 6.1 前置通知（Before）

**源码位置**：[BeforeMethodInterceptor.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/interceptor/BeforeMethodInterceptor.java)

```java
public class BeforeMethodInterceptor implements MethodInterceptor {
    private Method beforeMethod;
    private Object aspectInstance;
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 执行前置通知
        beforeMethod.invoke(aspectInstance);
        
        // 继续执行下一个拦截器或目标方法
        return invocation.proceed();
    }
}
```

### 6.2 后置通知（After）

**源码位置**：[AfterMethodInterceptor.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/interceptor/AfterMethodInterceptor.java)

```java
public class AfterMethodInterceptor implements MethodInterceptor {
    private Method afterMethod;
    private Object aspectInstance;
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            // 执行目标方法
            return invocation.proceed();
        } finally {
            // 无论是否异常都执行后置通知
            afterMethod.invoke(aspectInstance);
        }
    }
}
```

### 6.3 环绕通知（Around）

**源码位置**：[AroundMethodInterceptor.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/interceptor/AroundMethodInterceptor.java)

```java
public class AroundMethodInterceptor implements MethodInterceptor {
    private Method aroundMethod;
    private Object aspectInstance;
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 将ProceedingJoinPoint传入通知方法
        ProceedingJoinPoint pjp = new ProceedingJoinPoint(
            invocation.getTarget(), invocation.getMethod(), 
            invocation.getArgs(), invocation.getProxy());
        
        // 执行环绕通知方法，传入pjp让它控制proceed
        return aroundMethod.invoke(aspectInstance, pjp);
    }
}
```

### 6.4 通知执行顺序

```
调用代理方法
    ↓
@Before通知（BeforeMethodInterceptor）
    执行before逻辑
    ↓
@Around通知前置逻辑（AroundMethodInterceptor）
    执行around before逻辑
    调用pjp.proceed()
    ↓
目标方法执行
    ↓
@Around通知后置逻辑
    执行around after逻辑
    ↓
@After通知（AfterMethodInterceptor）
    执行after逻辑（finally块）
    ↓
返回结果
```

### 6.5 与Spring官方对比

**Spring官方的通知类型**：

```
Advice（通知顶级接口）
├── BeforeAdvice（前置通知）
│   └── MethodBeforeAdvice
├── AfterAdvice（后置通知）
│   ├── AfterReturningAdvice（返回后通知）
│   └── ThrowsAdvice（异常通知）
└── Interceptor（拦截器）
    └── MethodInterceptor（环绕通知）
```

**LightSSM的通知类型**：

```
MethodInterceptor（统一拦截器接口）
├── BeforeMethodInterceptor（前置通知）
├── AfterMethodInterceptor（后置通知）
└── AroundMethodInterceptor（环绕通知）
```

**差异分析**：

| 通知类型 | Spring官方 | LightSSM | 说明 |
|---------|-----------|----------|------|
| Before | MethodBeforeAdvice | BeforeMethodInterceptor | Spring有专门接口 |
| AfterReturning | AfterReturningAdvice | 无 | LightSSM不支持 |
| AfterThrowing | ThrowsAdvice | 无 | LightSSM不支持 |
| After | AfterAdvice | AfterMethodInterceptor | finally块实现 |
| Around | MethodInterceptor | AroundMethodInterceptor | 一致 |

## 7. 切入点表达式

### 7.1 AspectJ表达式语法

AspectJ表达式用于匹配连接点（方法）：

```
execution(modifiers-pattern? ret-type-pattern declaring-type-pattern? name-pattern(param-pattern) throws-pattern?)

示例：
execution(* com.example.service.*.*(..))     // service包下所有类的所有方法
execution(public * *(..))                    // 所有public方法
execution(* set*(..))                        // 所有set开头的方法
execution(* com.example.service.UserService.*(..))  // UserService的所有方法
```

### 7.2 LightSSM的实现

**源码位置**：[AspectJExpressionPointcut.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/pointcut/AspectJExpressionPointcut.java)

```java
public class AspectJExpressionPointcut {
    
    private String expression;
    private Pattern pattern;
    
    public AspectJExpressionPointcut(String expression) {
        this.expression = expression;
        parseExpression();
    }
    
    protected String convertExpressionToRegex(String expression) {
        if (expression.startsWith("execution(")) {
            String content = expression.substring("execution(".length(), 
                expression.length() - 1);
            
            content = content.replace("*", ".*");      // * -> .*
            content = content.replace("..", ".*");     // .. -> .*
            content = content.replace("(", "\\(");     // ( -> \(
            content = content.replace(")", "\\)");     // ) -> \)
            
            return content;
        }
        
        return expression.replace("*", ".*").replace("..", ".*");
    }
    
    public boolean matches(Method method) {
        String methodSignature = method.getDeclaringClass().getName() 
            + "." + method.getName();
        return pattern.matcher(methodSignature).matches();
    }
}
```

### 7.3 表达式转换示例

```
AspectJ表达式：execution(* com.example.service.*.*(..))

转换过程：
1. 提取execution()内容：* com.example.service.*.*(..)
2. * -> .*：.* com.example.service.*.*(..)
3. .. -> .*：.* com.example.service.*.*(.*)
4. ( -> \(：.* com.example.service.*.*\(.*)
5. ) -> \)：.* com.example.service.*.*\(.*)

最终正则：.* com.example.service.*.*\(.*)
```

### 7.4 与Spring官方对比

**Spring官方使用AspectJ Weaver库**：

```java
// Spring官方使用org.aspectj.weaver.tools.PointcutParser
public class AspectJExpressionPointcut implements Pointcut {
    private PointcutExpression pointcutExpression;
    
    public AspectJExpressionPointcut(String expression) {
        PointcutParser parser = PointcutParser
            .getPointcutParserSupportingSpecifiedPrimitives();
        this.pointcutExpression = parser.parsePointcutExpression(expression);
    }
    
    public boolean matches(Method method, Class<?> targetClass) {
        ShadowMatch shadowMatch = pointcutExpression.matchesMethodInvocation(method);
        return shadowMatch.alwaysMatches();
    }
}
```

**对比分析**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 解析器 | AspectJ Weaver | 正则表达式 |
| 支持的语法 | 完整AspectJ语法 | 基础execution语法 |
| 性能 | 较高（AST解析） | 较低（正则匹配） |
| 类型匹配 | 支持args/within/this等 | 仅支持execution |
| 动态匹配 | 支持运行时参数匹配 | 不支持 |

## 8. AdvisedSupport配置管理

### 8.1 LightSSM的实现

**源码位置**：[AdvisedSupport.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/aop/core/AdvisedSupport.java)

```java
public class AdvisedSupport {
    private Object target;                                    // 目标对象
    private Class<?> targetClass;                             // 目标类
    private Map<Method, List<MethodInterceptor>> methodInterceptors; // 方法拦截器映射
    
    public void addInterceptor(Method method, MethodInterceptor interceptor) {
        List<MethodInterceptor> interceptors = this.methodInterceptors
            .computeIfAbsent(method, k -> new ArrayList<>());
        interceptors.add(interceptor);
    }
    
    public List<MethodInterceptor> getInterceptors(Method method) {
        return this.methodInterceptors.get(method);
    }
}
```

### 8.2 与Spring官方对比

**Spring官方的AdvisedSupport**：

```java
public class AdvisedSupport extends ProxyConfig implements Advised {
    private TargetSource targetSource;        // 目标源（支持热切换）
    private MethodMatcher methodMatcher;      // 方法匹配器
    private AdvisorChainFactory advisorChainFactory; // 拦截器链工厂
    private List<Advisor> advisors;           // 通知器列表
    private Map<MethodCacheKey, List<Object>> methodCache; // 方法缓存
    private boolean exposeProxy = false;      // 是否暴露代理
    private boolean opaque = false;           // 是否不透明
    private boolean frozen = false;           // 是否冻结配置
    // 50+个属性和方法
}
```

**Advisor vs MethodInterceptor**：

```
Spring官方：
Advisor（通知器）= Pointcut + Advice
    ↓
AdvisorChainFactory根据Pointcut匹配方法，获取Advice
    ↓
Advice转换为MethodInterceptor执行

LightSSM：
Method（方法）直接映射到List<MethodInterceptor>
    ↓
简化了Advisor和Pointcut的匹配过程
```

## 9. 代理对象暴露

### 9.1 AopContext机制

在某些场景下，目标对象需要访问自己的代理对象（如内部方法调用也需要AOP增强）：

```java
@Service
public class UserService {
    
    public void methodA() {
        // 直接调用methodB，不会经过AOP代理
        methodB();
        
        // 通过AopContext获取代理对象调用
        ((UserService) AopContext.currentProxy()).methodB();
    }
    
    @Transactional
    public void methodB() { ... }
}
```

### 9.2 Spring官方的实现

```java
public abstract class AopContext {
    private static final ThreadLocal<Object> currentProxy = new ThreadLocal<>();
    
    public static Object currentProxy() throws IllegalStateException {
        Object proxy = currentProxy.get();
        if (proxy == null) {
            throw new IllegalStateException("Cannot find current proxy");
        }
        return proxy;
    }
    
    static Object setCurrentProxy(Object proxy) {
        Object old = currentProxy.get();
        currentProxy.set(proxy);
        return old;
    }
}
```

### 9.3 LightSSM的支持

LightSSM的MethodInvocation中保存了proxy引用，可以通过`getProxy()`方法获取：

```java
public class MethodInvocation {
    private Object proxy;  // 代理对象
    
    public Object getProxy() {
        return this.proxy;
    }
}
```

## 10. 设计模式总结

### 10.1 代理模式

```java
// 抽象主题
public interface AopProxy {
    Object getProxy();
}

// JDK代理
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler { ... }

// CGLIB代理
public class CglibAopProxy implements AopProxy { ... }
```

### 10.2 责任链模式

```java
public class MethodInvocation {
    private List<MethodInterceptor> interceptors;
    private int currentInterceptorIndex = -1;
    
    public Object proceed() throws Throwable {
        currentInterceptorIndex++;
        if (currentInterceptorIndex < interceptors.size()) {
            return interceptors.get(currentInterceptorIndex).invoke(this);
        }
        return method.invoke(target, args);
    }
}
```

### 10.3 工厂模式

```java
public class ProxyFactory {
    protected AopProxy createAopProxy() {
        // 根据目标类选择代理策略
        if (targetClass.getInterfaces().length > 0) {
            return new JdkDynamicAopProxy(advised);
        } else {
            return new CglibAopProxy(advised);
        }
    }
}
```

### 10.4 策略模式

```java
// AOP代理策略
AopProxy strategy1 = new JdkDynamicAopProxy(advised);
AopProxy strategy2 = new CglibAopProxy(advised);
```

## 11. 面试考点

### 11.1 高频问题

1. **Spring AOP的实现原理是什么？**
   - JDK动态代理（基于接口）
   - CGLIB代理（基于子类）

2. **JDK动态代理和CGLIB的区别？**
   - JDK代理接口，CGLIB代理类
   - JDK使用Proxy，CGLIB使用Enhancer
   - Spring Boot 2.x后默认CGLIB

3. **Spring AOP支持哪些通知类型？**
   - Before、AfterReturning、AfterThrowing、After、Around

4. **什么是AopContext？什么场景下使用？**
   - 用于在目标对象内部访问代理对象
   - 内部方法调用也需要AOP增强时

### 11.2 深度问题

1. **Spring AOP为什么使用责任链模式？**
   - 多个通知需要按顺序执行
   - 支持拦截器的灵活组合

2. **MethodInvocation的proceed()是如何工作的？**
   - 递归调用，索引递增
   - 所有拦截器执行完后调用目标方法

3. **AspectJ表达式是如何解析的？**
   - Spring官方：AspectJ Weaver库（AST解析）
   - LightSSM：正则表达式（简化实现）

## 12. 总结

LightSSM的AOP模块完整实现了Spring AOP的核心机制：

- **双代理策略**：JDK动态代理 + CGLIB代理
- **拦截器链**：责任链模式实现通知执行
- **三种通知**：Before、After、Around
- **切入点匹配**：AspectJ表达式解析

通过对照Spring官方源码，可以看到LightSSM保留了AOP的核心逻辑，剥离了高级特性（如动态匹配、引介增强等），非常适合学习理解AOP原理。

---

**上一篇**：[02 - IoC容器核心实现与Spring源码对比](02-IoC容器核心实现与Spring源码对比.md)

**下一篇**：[04 - MVC框架实现与Spring MVC源码对比](04-MVC框架实现与SpringMVC源码对比.md)
