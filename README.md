# Light Framework - 轻量级SSM框架

## 项目简介

Light Framework 是一个轻量级SSM框架，完整实现了IoC容器、SpringMVC、ORM和AOP核心功能，适合用于学习Spring框架原理和简历展示。

## 核心特性

### 1. IoC/DI容器
- ✅ 支持 `@Component`、`@Autowired` 注解扫描与依赖注入
- ✅ **基于三级缓存解决循环依赖问题**
- ✅ 支持 `@Scope` 注解配置单例/原型模式

### 2. SpringMVC核心
- ✅ 实现 `DispatcherServlet`、`HandlerMapping`、`HandlerAdapter`
- ✅ 支持 `@RequestMapping`、`@ResponseBody` 开发RESTful接口
- ✅ 支持 `@RequestParam`、`@PathVariable` 参数绑定
- ✅ 实现ViewResolver视图解析器

### 3. ORM模块
- ✅ 支持XML SQL映射配置
- ✅ 支持动态SQL拼接（OGNL表达式）
- ✅ 参数自动绑定与ResultSet映射
- ✅ 支持插件拦截器机制

### 4. AOP切面
- ✅ **基于JDK/CGLIB双代理实现**
- ✅ 支持 `@Before`、`@After`、`@Around` 通知织入
- ✅ 支持AspectJ表达式切入点

## 技术架构

```
com.lightframework
├── ioc/                    # IoC容器模块
│   ├── annotation/         # 注解定义
│   ├── beans/              # Bean定义
│   ├── core/               # 核心接口与实现
│   ├── context/            # 应用上下文
│   └── exception/          # 异常处理
├── mvc/                    # SpringMVC模块
│   ├── annotation/         # MVC注解
│   ├── core/               # 核心接口
│   ├── handler/            # 处理器映射与适配
│   ├── servlet/            # DispatcherServlet
│   └── view/               # 视图解析
├── orm/                    # ORM模块
│   ├── session/            # 会话管理
│   ├── executor/           # SQL执行器
│   ├── binding/            # Mapper代理
│   ├── mapping/            # SQL映射
│   └── type/               # 类型处理
└── aop/                    # AOP模块
    ├── annotation/         # AOP注解
    ├── core/               # 核心接口
    ├── interceptor/        # 方法拦截器
    └── pointcut/           # 切入点表达式
```

## 快速开始

### 1. IoC容器使用

```java
// 创建应用上下文
ApplicationContext context = new AnnotationConfigApplicationContext("com.example");

// 获取Bean
UserService userService = context.getBean(UserService.class);
```

### 2. SpringMVC使用

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

### 3. ORM使用

```xml
<mapper namespace="UserMapper">
    <select id="selectUserById" resultType="com.example.User">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

### 4. AOP使用

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

## 设计模式

| 设计模式 | 应用位置 |
|----------|----------|
| 工厂模式 | SqlSessionFactory、BeanFactory |
| 代理模式 | MapperProxy、JdkDynamicAopProxy |
| 模板方法 | BaseExecutor |
| 建造者模式 | MappedStatement.Builder |
| 策略模式 | TypeHandler |
| 责任链模式 | InterceptorChain、MethodInterceptor |

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

## Git提交记录

本项目按照开发阶段分6次提交，每次提交都有详细的文档说明：

1. **stage1**: 项目结构初始化（3个文件，205行）
2. **stage2**: IoC容器核心实现（16个文件，940行）
3. **stage3**: SpringMVC核心实现（20个文件，976行）
4. **stage4**: ORM模块完善（159个文件，16,584行）
5. **stage5**: AOP切面实现（19个文件，525行）
6. **stage6**: 文档和README（本提交）

## License

MIT License