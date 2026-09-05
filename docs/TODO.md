# LightSSM TODO 总览

> 自动生成于 2026-09-04（第二轮），扫描 `src/main/**/*.java` 中的 `// TODO [Lx][分类]` 标记。
> 约定：每条 TODO 末尾的 `；写对标志：...` 给出可操作的验收标准（如何判断“写对了”）。

**条目总数：161**　等级 L1=57 / L2=55 / L3=49　带验收标准（写对标志）：161/161

## 按分类

- **优化-工厂方法**：11 条
- **优化-建造者**：1 条
- **优化-模板方法**：5 条
- **优化-策略模式**：9 条
- **优化-观察者模式**：3 条
- **优化-责任链**：5 条
- **优化-适配器**：1 条
- **特性**：3 条
- **练习**：121 条
- **缺陷**：2 条

## 按模块

- `mvc`：46 条
- `ioc`：44 条
- `aop`：42 条
- `di`：27 条
- `orm`：2 条

## 条目清单

| 分类 | 等级 | 模块 | 位置 | 描述 | 验收 |
|------|------|------|------|------|------|
| 练习 | L1 | aop | `aop/annotation/After.java:9` | 理解 @After 与 @AfterReturning/@AfterThrowing 的区别——@After 在 finally 中执行(无论成功... | ✔ |
| 练习 | L1 | aop | `aop/annotation/AfterReturning.java:9` | 理解 @AfterReturning.returning()——需把目标返回值绑定到通知参数；写下目标返回值类型与通知参数；写对标志：实现后运行本... | ✔ |
| 练习 | L1 | aop | `aop/annotation/AfterThrowing.java:9` | 理解 @AfterThrowing.throwing()——需把异常绑定到通知参数；写下异常类型与通知参数类型不匹配时；写对标志：实现后运行本类/... | ✔ |
| 练习 | L1 | aop | `aop/annotation/Around.java:9` | 理解 @Around 约束——@Around 通知方法必须返回 Object 且首参为 ProceedingJoinPoint；写对标志：实现后运... | ✔ |
| 练习 | L1 | aop | `aop/annotation/Aspect.java:12` | 理解 @Aspect 被 @Component 元注解修饰——IOC 扫描时应把切面也注册为 Bean；写测试验证带 @Aspect；写对标志：实... | ✔ |
| 练习 | L1 | aop | `aop/annotation/AspectJAutoProxyCreator.java:302` | resolvePointcut 当 value 是命名切点(如 "servicePointcut")时需从 pointcutMap 取出；写对标志... | ✔ |
| 练习 | L1 | aop | `aop/annotation/Before.java:9` | 为 @Before 增加 pointcut 别名属性(如 value 与 pointcut 二选一，类似 Spring 的 @AliasFor)，... | ✔ |
| 练习 | L1 | aop | `aop/annotation/Pointcut.java:9` | 理解 @Pointcut 命名切点——advice 的 value 引用该命名方法(如 "servicePC()")时需；写对标志：实现后运行本类... | ✔ |
| 练习 | L1 | aop | `aop/core/AdvisedSupport.java:62` | 理解 addInterceptor/addInterceptors 在写入后会置 compiled=false 触发下次查找重新 compile(... | ✔ |
| 练习 | L1 | aop | `aop/core/CglibAopProxy.java:90` | 理解 handleObjectMethod 对 CGLIB 代理 Object 方法的透传——当前 equals/hashCode/toStrin... | ✔ |
| 练习 | L1 | aop | `aop/core/JoinPoint.java:83` | 理解 JoinPoint.getSignature()/getArgsString() 的构建——当前用 targetClass.methodNa... | ✔ |
| 练习 | L1 | aop | `aop/core/MethodInvocation.java:161` | 理解 setArgs/reset 如何同步刷新 JoinPoint 的参数——当前 setArgs 会顺带更新 joinPoint.args；写对... | ✔ |
| 练习 | L1 | aop | `aop/core/ProceedingJoinPoint.java:28` | 理解 ProceedingJoinPoint.proceed() 如何委托到 MethodInvocation.proceed()；写对标志：实现... | ✔ |
| 练习 | L1 | aop | `aop/interceptor/BeforeMethodInterceptor.java:27` | 在构造中理解 MethodHandle 适配——当前按参数个数决定是否 asType 为 (JoinPoint) 或 ()；写对标志：实现后运行本... | ✔ |
| 练习 | L1 | di | `di/annotation/Autowired.java:9` | 手写 @Autowired 的 required 语义——当 required()=true 且容器中无匹配依赖时注入引擎应抛 Dependenc... | ✔ |
| 练习 | L1 | di | `di/annotation/Resource.java:9` | 手写 @Resource 的 name/type 解析规则说明与判定——name 属性非空时优先按名称查找，否则按字段/参数类型（type 属性或... | ✔ |
| 练习 | L1 | di | `di/annotation/Value.java:9` | 手写 @Value 占位符提取工具——从注解 value 中正则提取 ${key:default} 的 key 与 default（如 "${ap... | ✔ |
| 练习 | L1 | di | `di/core/AnnotationInjectEntry.java:47` | 手写便捷构造函数——仅类型+注入器（或再加字段）的构造函数应委托主构造函数，并把 qualifier 置 ""、required 置 true、l... | ✔ |
| 练习 | L1 | di | `di/core/DefaultTypeConverter.java:40` | 手写「源对象已是目标类型则直接短路返回」的判断——targetType.isInstance(source) 为真时直接 targetType.c... | ✔ |
| 练习 | L1 | di | `di/core/DefaultTypeConverter.java:69` | 手写枚举解析分支——targetType.isEnum() 时用 Enum.valueOf((Class<Enum>) targetType, v... | ✔ |
| 练习 | L1 | di | `di/core/InjectionEngine.java:78` | 实现 @Lazy 代理注入分支——当 entry.lazyProxy 不为 null 时直接注入该代理对象，跳过实时依赖解析；写对标志：注入后字段... | ✔ |
| 练习 | L1 | di | `di/core/InjectionEngine.java:159` | 手写 @Resource 方法（setter）注入——校验方法参数个数为 1，用 resolveResourceByNameOrType 取到依赖... | ✔ |
| 练习 | L1 | di | `di/core/InjectionEngine.java:237` | 手写 @Value 字段注入的「占位符解析 + 类型转换」衔接——先用 placeholderResolver.resolvePlaceholde... | ✔ |
| 练习 | L1 | di | `di/core/InjectionMetadata.java:36` | 手写字段级去重——维护一个 Set<Field>，提供 markFieldInjected(field) 记录已注入、isFieldInjecte... | ✔ |
| 练习 | L1 | di | `di/core/InjectionMetadata.java:51` | 手写方法级去重——维护一个 Set<Method>，提供 markMethodInjected(method)/isMethodInjected(... | ✔ |
| 练习 | L1 | di | `di/core/PlaceholderResolver.java:3` | 手写一个 PlaceholderResolver 实现——从 Properties/环境变量/System.getProperties 中按 ke... | ✔ |
| 练习 | L1 | ioc | `ioc/beans/BeanDefinition.java:187` | 手写 BeanDefinition 的 equals/hashCode（以 beanName 为主、beanClass 为辅，保证容器中两个同名 ... | ✔ |
| 练习 | L1 | ioc | `ioc/context/ClassPathBeanDefinitionScanner.java:352` | 手写 Bean 名称生成 generateBeanName（首字母小写；连续大写如 "URL" 保持原样）。；写对标志：实现后运行本类/本包对应单... | ✔ |
| 练习 | L1 | ioc | `ioc/core/BeanDefinitionRegistry.java:10` | 手写 BeanDefinitionRegistry（用 Map 存 BeanDefinition + 别名映射，registerAlias 需处理... | ✔ |
| 练习 | L1 | ioc | `ioc/core/BeanFactory.java:4` | 手写最简 BeanFactory 实现（用 Map<String,Object> 持有单例，getBean(name) 直接返回，getBean(... | ✔ |
| 练习 | L1 | ioc | `ioc/core/BeanLifecycleManager.java:95` | 手写自定义生命周期方法调用 invokeCustomMethod（按名反射调用 initMethod/destroyMethod，找不到时按 ph... | ✔ |
| 练习 | L1 | ioc | `ioc/core/BeanPostProcessor.java:4` | 手写一个 BeanPostProcessor（如给所有 Bean 统一设置某字段，或用代理包装），理解 BPP 在初始化前后对 Bean 的拦截。... | ✔ |
| 练习 | L1 | ioc | `ioc/core/FastBeanLookup.java:52` | 手写 O(1) 快速查找 lookup（用开放寻址哈希表替代当前小数组线性扫描，正确处理 hash 冲突）。；写对标志：实现后运行本类/本包对应单... | ✔ |
| 练习 | L1 | ioc | `ioc/core/PropertyPlaceholderConfigurer.java:178` | 手写 ${key:default} 占位符解析 resolvePlaceholderKey（拆分 key 与默认值，解析嵌套占位符，找不到且默认缺... | ✔ |
| 练习 | L1 | ioc | `ioc/core/SingletonCache.java:80` | 手写 addSingleton（把完整实例放入一级缓存 singletonObjects，并移除二、三级缓存、标记已创建）。；写对标志：实现后运行... | ✔ |
| 练习 | L1 | ioc | `ioc/core/SingletonCache.java:160` | 手写逆序销毁 destroySingletons（按 singletonObjects 的注册逆序执行 destroyCallback）。；写对标... | ✔ |
| 练习 | L1 | ioc | `ioc/core/StandardEnvironment.java:45` | 手写 acceptsProfiles（参数为空返回 true；任一给定 profile 命中 activeProfiles 即返回 true）。；... | ✔ |
| 练习 | L1 | ioc | `ioc/event/ApplicationEvent.java:16` | 手写 ApplicationEvent 的 equals/hashCode（按 source + timestamp 比较；或仅按 source，... | ✔ |
| 练习 | L1 | ioc | `ioc/exception/NoSuchBeanDefinitionException.java:52` | 手写 Levenshtein 距离 levenshteinDistance（经典 DP，用于给"找不到 Bean"的错误提供相似名称建议）。；写对... | ✔ |
| 练习 | L1 | mvc | `mvc/convert/ConverterRegistry.java:12` | 仿照 StringToIntegerConverter，新增一个 StringToBigDecimalConverter（或 StringToLo... | ✔ |
| 练习 | L1 | mvc | `mvc/core/CorsProcessor.java:169` | 把 isOriginAllowed 中的"后缀通配匹配"逻辑（allowed.startsWith("*.") && host.endsWith(... | ✔ |
| 练习 | L1 | mvc | `mvc/core/HandlerExecutionChain.java:35` | 当前某个 preHandle 返回 false 时调用 triggerAfterCompletion(request, response, nul... | ✔ |
| 练习 | L1 | mvc | `mvc/core/HandlerInterceptor.java:7` | 默认三个方法都是空/true 实现；请手写一个拦截器（如日志、登录鉴权、耗时统计），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言... | ✔ |
| 练习 | L1 | mvc | `mvc/core/ModelAndView.java:34` | addAllObjects 直接 putAll 会覆盖同名 key；请改为"遇到已存在的 key 时合并/忽略"的可配置策略，；写对标志：实现后运... | ✔ |
| 练习 | L1 | mvc | `mvc/core/ModelAndView.java:61` | 当前 isReference() 只识别 "redirect:" 前缀；请同时识别 "forward:" 前缀（forward 视图也是"引用"而... | ✔ |
| 练习 | L1 | mvc | `mvc/handler/ExceptionHandlerRegistry.java:26` | 当前按 Map 遍历顺序做 isAssignableFrom 匹配，命中第一个父类，无法保证"最具体的异常类型优先"；写对标志：实现后运行本类/本... | ✔ |
| 练习 | L1 | mvc | `mvc/handler/HandlerMethod.java:60` | HandlerMethod 目前没有 equals/hashCode；请基于 (beanType, method) 实现它们（注意 method ... | ✔ |
| 练习 | L1 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:366` | convertValue 对 int/long/double/boolean 用了多条 if 链；请改为"注册式/查表"转换；写对标志：实现后运行... | ✔ |
| 练习 | L1 | mvc | `mvc/handler/RequestMappingHandlerMapping.java:149` | 类级 @RequestMapping 前缀拼接（补前导 "/"，去尾部 "/"）目前逻辑分散；请抽取为 normalizePath(path) 工... | ✔ |
| 练习 | L1 | mvc | `mvc/handler/RequestMappingInfo.java:31` | 当前 {var} 只编译成 ([^/]+)；请支持可选变量 {var?}（正则用 (/[^/]+)?）与带约束变量 {var:\\d+}（用其中的... | ✔ |
| 练习 | L1 | mvc | `mvc/multipart/MultipartFile.java:46` | 补充 transferTo(java.io.File dest) 便捷方法，将 bytes 写入目标文件，并在 bytes 为空时按 isEmpt... | ✔ |
| 练习 | L1 | mvc | `mvc/servlet/DispatcherServlet.java:158` | 把"遍历 handlerMappings 取第一个非空 HandlerExecutionChain"的查找逻辑，；写对标志：实现后运行本类/本包对... | ✔ |
| 练习 | L1 | mvc | `mvc/servlet/DispatcherServlet.java:230` | 把"遍历 viewResolvers 取第一个非空 View"的查找逻辑抽取为 lookupViewResolver(viewName) 辅助方法... | ✔ |
| 练习 | L1 | mvc | `mvc/view/InternalResourceView.java:20` | 当前把 model 的每个 entry 直接 setAttribute 到 request；请明确"已存在同名 request attribute... | ✔ |
| 练习 | L1 | mvc | `mvc/view/RedirectView.java:24` | 当前重定向 URL 未保留原始查询串、也未对非 ASCII 做 URL 编码；请补充：当 redirectUrl 不含查询串时拼接原请求的 que... | ✔ |
| 缺陷 | L1 | aop | `aop/annotation/AspectJAutoProxyCreator.java:80` | 惰性解析导致"切面须先于目标创建"：若目标 Bean 在 @Aspect Bean 之前实例化，；写对标志：先写一个“修复前失败、修复后通过”的复... | ✔ |
| 缺陷 | L1 | ioc | `ioc/context/AnnotationConfigApplicationContext.java:647` | 本方法早于 processBeanMethods() 运行，因此 @Configuration 中 @Bean 声明的；写对标志：先写一个“修复前... | ✔ |
| 优化-工厂方法 | L2 | ioc | `ioc/context/AnnotationConfigApplicationContext.java:397` | 与 evaluateConditions 中的匿名 ConditionContext 完全相同：应抽取 createConditionContex... | ✔ |
| 优化-模板方法 | L2 | aop | `aop/core/AdvisedSupport.java:83` | findChainIndex 的"快速表 → 线性搜索 → equals 回退 → 接口-实现回退"四段查找流程；写对标志：按模板方法模式完成实现... | ✔ |
| 优化-适配器 | L2 | mvc | `mvc/servlet/DispatcherServlet.java:171` | HandlerAdapter 已用适配器模式（HandlerAdapter 接口 + supports/handle）。；写对标志：按适配器模式完... | ✔ |
| 练习 | L2 | aop | `aop/annotation/AspectJAutoProxyCreator.java:115` | computeClassMatchMask 当前用 int 位掩码(bitIndex<31)只支持最多 31 个 advice，；写对标志：实现后... | ✔ |
| 练习 | L2 | aop | `aop/annotation/AspectJAutoProxyCreator.java:236` | 在 parseAspect 中把同一切面类里的 @Before/@After/@Around/@AfterReturning/@AfterThro... | ✔ |
| 练习 | L2 | aop | `aop/core/AdvisedSupport.java:142` | 实现 compile() 中 fastLookupTable 的开放寻址冲突处理——当前 hash 冲突时只把第一个空槽；写对标志：实现后运行本类... | ✔ |
| 练习 | L2 | aop | `aop/core/CglibAopProxy.java:40` | 实现 CGLIB 代理类缓存——当前 enhancer.create() 每次都重新生成字节码；写对标志：实现后运行本类/本包对应单测（无则新建一... | ✔ |
| 练习 | L2 | aop | `aop/core/JdkDynamicAopProxy.java:47` | 完善 Object 方法(guarded)处理——当前已处理 equals/hashCode/toString，；写对标志：实现后运行本类/本包对... | ✔ |
| 练习 | L2 | aop | `aop/core/JoinPoint.java:31` | 实现 JoinPoint.obtain 的嵌套对象池保护——与 MethodInvocation 同理，POOL_SIZE=8 超出时；写对标志：... | ✔ |
| 练习 | L2 | aop | `aop/core/MethodInvocation.java:32` | 实现 obtain/release 对象池的嵌套层数保护——当前 POOL_SIZE=8 硬编码，超出后才 new 新实例；写对标志：实现后运行本... | ✔ |
| 练习 | L2 | aop | `aop/core/ProceedingJoinPoint.java:39` | 实现 proceed(Object[] args) 的参数改写——当前 setArgs 后 proceed，请验证改写后的 args；写对标志：实... | ✔ |
| 练习 | L2 | aop | `aop/core/ProxyFactory.java:29` | 补充代理策略选择——当目标类 final 且无接口、且 preferCglib=false 时给出明确降级/报错；写对标志：实现后运行本类/本包对... | ✔ |
| 练习 | L2 | aop | `aop/interceptor/AfterMethodInterceptor.java:36` | 实现 @After 的 finally 语义——当前 try{ proceed } finally{ 执行 after 通知 }；写对标志：实现后... | ✔ |
| 练习 | L2 | aop | `aop/interceptor/AfterReturningMethodInterceptor.java:27` | 实现 @AfterReturning——当前 proceed() 取返回值后执行通知(仅传 JoinPoint)；请补全；写对标志：实现后运行本类... | ✔ |
| 练习 | L2 | aop | `aop/interceptor/AfterThrowingMethodInterceptor.java:27` | 实现 @AfterThrowing——当前 catch 中执行通知再抛出原异常；请补全 throwing() 属性把异常对象；写对标志：实现后运行... | ✔ |
| 练习 | L2 | aop | `aop/interceptor/AroundMethodInterceptor.java:36` | 实现 @Around 对 ProceedingJoinPoint 的包装——当前 new ProceedingJoinPoint(invocati... | ✔ |
| 练习 | L2 | aop | `aop/interceptor/BeforeMethodInterceptor.java:43` | 实现 @Before 的通知执行与链推进——当前 takesJoinPoint 时传 joinPoint 否则无参调用；写对标志：实现后运行本类/... | ✔ |
| 练习 | L2 | aop | `aop/pointcut/AspectJExpressionPointcut.java:57` | 实现 parseExpression 对 execution 表达式的完整解析——当前按"返回类型 包.类.方法(参数)"拆解并转；写对标志：实现... | ✔ |
| 练习 | L2 | aop | `aop/pointcut/AspectJExpressionPointcut.java:106` | 实现 convertParams 对参数通配符的处理——当前支持 * 与 ..；请补全泛型参数、数组参数(int[])、；写对标志：实现后运行本类... | ✔ |
| 练习 | L2 | di | `di/core/AnnotationInjectEntry.java:38` | 手写 fieldName 的预计算推导——field 不为 null 时取 field.getName()，为 null 时置 null；理解「预... | ✔ |
| 练习 | L2 | di | `di/core/DefaultTypeConverter.java:61` | 手写 String 到目标类型的分发逻辑——先查 converterTable 命中则 apply；未命中且目标为枚举则 Enum.valueOf... | ✔ |
| 练习 | L2 | di | `di/core/DefaultTypeConverter.java:85` | 手写自定义类型转换器的注册与接入——在 registerConverter 中把 (targetType, converter) 放入 custo... | ✔ |
| 练习 | L2 | di | `di/core/DependencyContainer.java:11` | 手写一个最小 DependencyContainer 实现 - 用 Map<String,Object> 按名称、Map<Class<?>,Obj... | ✔ |
| 练习 | L2 | di | `di/core/InjectionEngine.java:75` | 手写 @Resource 字段的「先按 name 再按 type」回退解析——entry.resourceName 非空且容器 containsB... | ✔ |
| 练习 | L2 | di | `di/core/InjectionEngine.java:115` | 手写 @Autowired 字段按类型解析——调用 container.resolveDependencyWithGenerics(field, ... | ✔ |
| 练习 | L2 | di | `di/core/InjectionEngine.java:198` | 手写 @Autowired 方法注入——取参数类型 paramType，结合 entry.qualifier/required 调用 contai... | ✔ |
| 练习 | L2 | di | `di/core/InjectionMetadata.java:29` | 手写「跨注解去重」的整体编排——一个字段同时被 @Resource 与 @Autowired 标记时，应只在第一次注入时执行并 mark，后续扫描... | ✔ |
| 练习 | L2 | ioc | `ioc/context/ClassPathBeanDefinitionScanner.java:288` | 手写 BeanDefinition 解析 registerOrCollectBeanDefinition（从类读取 @Scope/@Primary... | ✔ |
| 练习 | L2 | ioc | `ioc/core/BeanLifecycleManager.java:118` | 手写注解方法收集 collectAnnotatedMethods（递归遍历类层次，按 方法名+参数数 去重避免 override 重复执行 @Po... | ✔ |
| 练习 | L2 | ioc | `ioc/core/ConditionContext.java:8` | 手写 @Conditional 条件评估框架 —— 定义 Condition 接口 + 在注册 Bean 前调用 condition.matche... | ✔ |
| 练习 | L2 | ioc | `ioc/core/DefaultListableBeanFactory.java:155` | 手写类型索引的增量构建 indexTypeRecursive（递归收集 beanClass 的所有接口与父类到 typeIndex）。；写对标志：... | ✔ |
| 练习 | L2 | ioc | `ioc/core/DefaultListableBeanFactory.java:193` | 手写别名链解析 resolveAlias（支持链式别名 + 循环检测 + 最大深度限制）。；写对标志：实现后运行本类/本包对应单测（无则新建一个）... | ✔ |
| 练习 | L2 | ioc | `ioc/core/DefaultListableBeanFactory.java:460` | 手写按类型收集所有 Bean getBeansOfType（遍历 BeanDefinition，过滤 type.isAssignableFrom(... | ✔ |
| 练习 | L2 | ioc | `ioc/core/DefaultListableBeanFactory.java:800` | 手写 setter 注入 applyPropertyValues（扫描 setXxx 方法得到属性名，按名匹配 propertyValues 并反... | ✔ |
| 练习 | L2 | ioc | `ioc/core/DefaultListableBeanFactory.java:1347` | 手写 Bean 初始化骨架 initializeBean（依次：BPP.postProcessBeforeInitialization -> 调 ... | ✔ |
| 练习 | L2 | ioc | `ioc/core/FactoryBean.java:20` | 手写一个 FactoryBean（如简化版 SqlSessionFactoryBean：getObject() 创建目标对象，getObjectT... | ✔ |
| 练习 | L2 | ioc | `ioc/core/SingletonCache.java:104` | 手写循环依赖检测 beforeSingletonCreation（用 creationStack 记录正在创建的 Bean，重入同一 bean 时... | ✔ |
| 练习 | L2 | ioc | `ioc/core/health/IoCHealthChecker.java:23` | 手写容器健康检查 run（构建依赖图 -> 检测循环 -> 自动修复构造器环 -> 判定是否健康）。；写对标志：实现后运行本类/本包对应单测（无则... | ✔ |
| 练习 | L2 | ioc | `ioc/event/SimpleApplicationEventMulticaster.java:54` | 手写事件广播 publishEvent（遍历类型索引匹配监听器，支持父类事件类型命中，可选异步执行）。；写对标志：实现后运行本类/本包对应单测（无... | ✔ |
| 练习 | L2 | ioc | `ioc/event/SimpleApplicationEventMulticaster.java:131` | 手写监听器事件类型解析 resolveListenerEventType（从 ApplicationListener<T> 的泛型参数取出 T，找... | ✔ |
| 练习 | L2 | ioc | `ioc/scope/ApplicationScope.java:41` | 手写双重检查锁定缓存 get（name.intern() 作锁，首次创建后放入 beanCache，后续直接返回，保证单例）。；写对标志：实现后运... | ✔ |
| 练习 | L2 | ioc | `ioc/scope/Scope.java:19` | 手写一个自定义 ThreadScope 实现 Scope 接口（每个线程持有独立实例，get 时从 ThreadLocal 取/创建，remove... | ✔ |
| 练习 | L2 | ioc | `ioc/scope/WebScopeManager.java:49` | 手写线程安全单例 getInstance（双重检查锁定 + volatile 实例字段）。；写对标志：实现后运行本类/本包对应单测（无则新建一个）... | ✔ |
| 练习 | L2 | mvc | `mvc/core/ContentNegotiationManager.java:19` | 当前协商只看 Accept 头与 format 参数；请补充"按请求路径后缀协商"的能力（如 /a.json -> application/jso... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:47` | 本类分散维护 methodHandleCache / converterCache / exceptionHandlerCache / argsC... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:219` | 当前 DEFAULT 分支（无注解参数）只能按参数名从 request 取单个简单类型；写对标志：实现后运行本类/本包对应单测（无则新建一个），断... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:274` | 当前 @RequestBody 只支持 JSON（及 String 原文）；请补充对 application/x-www-form-urlenco... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:438` | 当前按 instanceof 顺序处理 ModelAndView / String / Map 返回值；请补充对常见返回类型的处理，；写对标志：实... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerMapping.java:117` | 当前组合注解（@GetMapping 等）只取了 path 与 method；请补充解析 RequestMapping 的；写对标志：实现后运行本... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingHandlerMapping.java:188` | 当前遍历 handlerMethods 用第一个 match 胜出，没有"匹配优先级"；请实现优先级：；写对标志：实现后运行本类/本包对应单测（无... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingInfo.java:33` | 当前只对 . * ( ) + ^ $ 做了转义；请补全对 ? 与 [ ] 等正则元字符的转义（或统一用 Pattern.quote 处理字面量段）... | ✔ |
| 练习 | L2 | mvc | `mvc/handler/RequestMappingInfo.java:63` | 当前 match 仅按路径模板与 HTTP 方法匹配；请支持附加条件（请求头、consumes/produces），；写对标志：实现后运行本类/本... | ✔ |
| 练习 | L2 | mvc | `mvc/multipart/MultipartResolver.java:141` | 当前 parsePart 只处理"有 filename 的文件域"；请补充处理"普通表单字段"（无 filename 的 part），；写对标志：... | ✔ |
| 练习 | L2 | mvc | `mvc/servlet/DispatcherServlet.java:191` | 当前 mv==null 直接 sendError(404)；补充对 ModelAndView.isReference() 的判断，；写对标志：实现... | ✔ |
| 练习 | L2 | mvc | `mvc/view/InternalResourceViewResolver.java:22` | 当前仅按 redirect:/forward: 前缀与默认拼接来选 View；请结合 ContentNegotiationManager 的结果选... | ✔ |
| 优化-工厂方法 | L3 | aop | `aop/core/AdvisedSupport.java:205` | AdvisedSupport 同时承担"拦截器存储(methodInterceptors)"与"编译索引/MethodHandle 预编译"；写对... | ✔ |
| 优化-工厂方法 | L3 | aop | `aop/core/ProxyFactory.java:33` | createAopProxy() 按条件返回 JdkDynamicAopProxy 或 CglibAopProxy 正是工厂方法模式；写对标志：按... | ✔ |
| 优化-工厂方法 | L3 | aop | `aop/interceptor/BeforeMethodInterceptor.java:15` | 五个通知拦截器(Before/After/Around/AfterReturning/AfterThrowing)构造里；写对标志：按工厂方法模式... | ✔ |
| 优化-工厂方法 | L3 | di | `di/core/DefaultTypeConverter.java:101` | 每种基本类型的 String 到对象转换目前用 Map + lambda 硬编码。可为每种类型定义一个转换器工厂方法（如 IntegerConve... | ✔ |
| 优化-工厂方法 | L3 | di | `di/core/DependencyContainer.java:6` | getBean(name,type) / getBean(type) / resolveDependency / resolveDependenc... | ✔ |
| 优化-工厂方法 | L3 | mvc | `mvc/convert/ConverterRegistry.java:7` | 当前转换器由调用方手动 addConverter 注册；可用工厂方法按 source/target 类型自动发现（SPI 扫描或注解标记），；写对... | ✔ |
| 优化-工厂方法 | L3 | mvc | `mvc/core/HandlerMapping.java:6` | 当前只有 RequestMappingHandlerMapping 一种实现；当引入静态资源映射、健康检查、WebSocket 等映射时，；写对标... | ✔ |
| 优化-工厂方法 | L3 | mvc | `mvc/handler/HandlerMethod.java:25` | 两个构造器语义不同（一个从容器按 beanName 取 Bean，一个直接持有 handler），容易被误用。；写对标志：按工厂方法模式完成实现，... | ✔ |
| 优化-工厂方法 | L3 | mvc | `mvc/handler/RequestMappingHandlerMapping.java:169` | 当前 RequestMappingInfo 的创建散落在多处（registerHandlerMethod、warmupHandlerCache、g... | ✔ |
| 优化-工厂方法 | L3 | mvc | `mvc/servlet/DispatcherServlet.java:121` | request 生命周期目前由 DispatcherServlet 直接管理；可抽象出 WebScope 接口（RequestScope/Sess... | ✔ |
| 优化-建造者 | L3 | di | `di/core/AnnotationInjectEntry.java:30` | AnnotationInjectEntry 有 8 个字段的构造函数，调用方极易把参数顺序传错。建议新增 AnnotationInjectEntr... | ✔ |
| 优化-模板方法 | L3 | aop | `aop/core/JdkDynamicAopProxy.java:18` | JdkDynamicAopProxy.invoke 与 CglibAopProxy.CglibMethodInterceptor.intercep... | ✔ |
| 优化-模板方法 | L3 | di | `di/core/InjectionEngine.java:24` | injectResourceFields / injectAutowiredFields / injectResourceMethods / in... | ✔ |
| 优化-模板方法 | L3 | ioc | `ioc/context/AnnotationConfigApplicationContext.java:160` | registerComponent 中读取 @Primary/@Qualifier/@Lazy/@DependsOn 的代码片段可抽取为统一的 a... | ✔ |
| 优化-模板方法 | L3 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:91` | handle() 三步骨架（解析参数 → 调用 → 处理返回值，异常走 handleException）已接近模板方法模式；写对标志：按模板方法模... | ✔ |
| 优化-策略模式 | L3 | aop | `aop/pointcut/AspectJExpressionPointcut.java:135` | matches(Class)/matches(Method)/matches(Class,Method) 三态匹配 + execution/@an... | ✔ |
| 优化-策略模式 | L3 | di | `di/core/InjectionEngine.java:47` | 当前 injectAll 硬编码按固定顺序调用 5 个私有注入方法。可抽象为 List<InjectionStrategy>（每个注解类型一个策略... | ✔ |
| 优化-策略模式 | L3 | ioc | `ioc/context/ClassPathBeanDefinitionScanner.java:238` | 下面多个 \|\| 的硬编码注解描述符扫描，可改成"候选注解描述符列表 + 循环"，或用注解元数据注册表，消除这段重复且易遗漏的 if 链。；写对标志... | ✔ |
| 优化-策略模式 | L3 | ioc | `ioc/core/DefaultListableBeanFactory.java:534` | doGetBean 中 singleton/prototype/customScope 三分支可用 Scope 策略统一：；写对标志：按策略模式完... | ✔ |
| 优化-策略模式 | L3 | ioc | `ioc/scope/ScopeRegistry.java:137` | destroyCustomScopes 中"排除内置作用域"的多个 && 判断，可用一个 BUILTIN_SCOPES 集合 + 单次 conta... | ✔ |
| 优化-策略模式 | L3 | mvc | `mvc/core/ContentNegotiationManager.java:45` | 当前用 switch 把 format 映射到 mediaType；可把每种映射抽象为 MediaTypeStrategy（如 JsonStrat... | ✔ |
| 优化-策略模式 | L3 | mvc | `mvc/core/CorsProcessor.java:112` | origin 是否放行的判定（精确匹配、"*"通配、后缀通配 "*.example.com"）目前写死在 if 里；写对标志：按策略模式完成实现，... | ✔ |
| 优化-策略模式 | L3 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:238` | 当前用 switch(meta.type()) 分发不同参数解析（RequestParam/PathVariable/RequestBody...... | ✔ |
| 优化-策略模式 | L3 | mvc | `mvc/view/ViewResolver.java:4` | 当前 ViewResolver 选择是 DispatcherServlet 里"遍历列表取第一个非空"；可把"按 viewName 前缀/后缀选择... | ✔ |
| 优化-观察者模式 | L3 | aop | `aop/annotation/AspectJAutoProxyCreator.java:42` | AspectJAutoProxyCreator 实现 BeanPostProcessor，在 IOC 容器；写对标志：按观察者模式完成实现，新增单... | ✔ |
| 优化-观察者模式 | L3 | di | `di/core/InjectionEngine.java:46` | 注入过程缺少生命周期钩子：可在「开始注入 / 每个字段注入成功 / 注入失败」时发布事件（如 InjectionEvent），让日志、监控、懒代理... | ✔ |
| 优化-观察者模式 | L3 | mvc | `mvc/handler/ExceptionHandlerRegistry.java:13` | 异常处理可改为观察者模式：异常发生时向已注册的 @ExceptionHandler 订阅者广播，；写对标志：按观察者模式完成实现，新增单测覆盖“主... | ✔ |
| 优化-责任链 | L3 | aop | `aop/annotation/AspectJAutoProxyCreator.java:188` | 拦截器链本身就是责任链；当前 getOrder() 只按拦截器类名前缀；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的... | ✔ |
| 优化-责任链 | L3 | aop | `aop/core/MethodInvocation.java:115` | MethodInvocation.proceed() 就是责任链推进核心：当前用 currentInterceptorIndex；写对标志：按责任... | ✔ |
| 优化-责任链 | L3 | di | `di/core/InjectionEngine.java:254` | 依赖解析目前只有「name→type」两级。可拆成责任链节点：LazyProxyNode → NameNode → TypeNode → Qual... | ✔ |
| 优化-责任链 | L3 | mvc | `mvc/core/HandlerExecutionChain.java:10` | 当前拦截器链已用责任链模式，但拦截器的装配（globalInterceptors 简单 add）缺少排序/条件匹配能力。；写对标志：按责任链模式完... | ✔ |
| 优化-责任链 | L3 | mvc | `mvc/handler/RequestMappingHandlerMapping.java:81` | 当前所有 globalInterceptors 无差别地挂到每个 chain；可引入"拦截器匹配规则"（按 path pattern / HTTP... | ✔ |
| 特性 | L3 | ioc | `ioc/core/DefaultListableBeanFactory.java:536` | 自定义作用域（request/session）Bean 当前直接返回 scope.get() 的真实实例；若某 singleton/prototy... | ✔ |
| 特性 | L3 | orm | `orm/executor/BaseExecutor.java:48` | BaseExecutor 每次都直接 doQuery，未利用 MappedStatement.useCache/flushCache 做一级缓存：... | ✔ |
| 特性 | L3 | orm | `orm/scripting/xmltags/XMLScriptBuilder.java:34` | 标准 9 种动态节点仅实现 trim/if，foreach、choose/when/otherwise、bind、where、set 尚未实现：在... | ✔ |
| 练习 | L3 | aop | `aop/annotation/AspectJAutoProxyCreator.java:143` | 修复拦截器链排序 bug，使 @Before/@After/@Around 按 Spring 语义正确织入；写对标志：实现后运行本类/本包对应单测... | ✔ |
| 练习 | L3 | aop | `aop/pointcut/AspectJExpressionPointcut.java:223` | 完善 @annotation 切点匹配——当前只按注解全限定名字符串逐一比较方法上的注解；写对标志：实现后运行本类/本包对应单测（无则新建一个），... | ✔ |
| 练习 | L3 | ioc | `ioc/context/AnnotationConfigApplicationContext.java:228` | 手写 @Import 解析 processImports（迭代处理 ImportSelector/ImportBeanDefinitionRegi... | ✔ |
| 练习 | L3 | ioc | `ioc/core/DefaultListableBeanFactory.java:690` | 手写构造器解析与实例化 instantiateBean。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常... | ✔ |
| 练习 | L3 | ioc | `ioc/core/DefaultListableBeanFactory.java:1402` | 手写泛型依赖解析 resolveDependencyWithGenerics（支持 List<T> 注入所有 T 类型 Bean、Map<Stri... | ✔ |
| 练习 | L3 | ioc | `ioc/core/DefaultListableBeanFactory.java:1591` | 手写基于 @DependsOn 的拓扑排序 topologicalSort（DFS 访问，记录 visited/inProgress，遇到 inP... | ✔ |
| 练习 | L3 | ioc | `ioc/core/SingletonCache.java:44` | 手写三级缓存查找 getSingleton（一级无锁快路径 -> 二级 earlySingletonObjects -> 三级 singleton... | ✔ |
| 练习 | L3 | ioc | `ioc/core/health/AutoFixer.java:27` | 手写构造器循环自动修复 fix（在环的断点 Bean 上标记需要 @Lazy 的构造器参数下标，使容器注入代理打破构造期循环）。；写对标志：实现后... | ✔ |
| 练习 | L3 | ioc | `ioc/core/health/CycleDetector.java:45` | 手写迭代版 Tarjan 强连通分量算法 tarjanIterative（用显式栈模拟递归 DFS，找出所有循环依赖环，避免大图递归 StackO... | ✔ |
| 练习 | L3 | ioc | `ioc/core/health/DependencyGraphBuilder.java:33` | 手写依赖图构建 build（建立 name->index 映射，遍历每个 Bean 的 @DependsOn/字段/构造器依赖生成邻接表，区分边类... | ✔ |
| 练习 | L3 | mvc | `mvc/core/CorsProcessor.java:55` | 预检请求当前只设置了允许的 Methods/Headers，但未校验本次请求的 Method 是否真的在 config.methods 列表中；写... | ✔ |
| 练习 | L3 | mvc | `mvc/handler/RequestMappingHandlerAdapter.java:105` | 当前异常只查找"当前 Controller 内"的 @ExceptionHandler；请补充 @ControllerAdvice 全局异常处理器... | ✔ |
| 练习 | L3 | mvc | `mvc/multipart/MultipartResolver.java:24` | 当前把整个请求体一次性读入 byte[]（readStream），既占内存也不支持嵌套多部分（multipart/mixed）；写对标志：实现后运... | ✔ |
| 练习 | L3 | mvc | `mvc/servlet/DispatcherServlet.java:118` | 在请求进入时把当前 request 绑定到 Web 作用域（例如调用 RequestScope.setCurrentRequest(request... | ✔ |
