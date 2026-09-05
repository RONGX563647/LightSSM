# LightSSM 手写练习路线

> 目标读者：想通过这个仓库练“手写代码 + 设计模式”、但代码经验还不多的人。
> 本文只讲**顺序和思路**；完整条目索引见 [`TODO.md`](TODO.md)，格式约定见 [`TODO-GUIDE.md`](TODO-GUIDE.md)。

## 0. 先理解 TODO 的两种形态

代码里的 `// TODO [Lx][分类]` 不全是“缺代码”：

- **已经实现的锚点**：TODO 下面就是正确实现，作用是让你在“关键机制”位置停下来想：这段为什么要这么写？练习时建议把实现先遮住或删掉，自己写一遍，再用测试验证。
- **真正的缺口**：TODO 下面只有注释或半成品，例如 MVC 请求/Web 作用域绑定、动态 SQL 节点、ORM 一级缓存。这些可以直接在源码里补。

无论哪种，都有 `写对标志/验收标准`，对应单测通常就在同名 `*Test.java` 里。

建议练习流程：

1. 先打开 TODO 所在源码，再看同包/同模块的 `*Test.java`，确认期望行为。
2. 动手写。源码已实现的，先在副本里删空对应方法再写，写完对照；源码确实缺的，直接在正式源码写。
3. 跑目标测试：

```bash
./mvn.sh -s ci-settings.xml -Dtest=DefaultTypeConverterTest test
```

全绿后再删掉那行 TODO（仅当你确实完成了重构/实现时才删），进入下一条。

## 1. 阶段一：先练“独立小方法”

目的：不依赖整个容器也能验证，适合建立信心。特征是**一个方法、一份输入、一个返回值**。

推荐顺序：

| 任务 | 看哪 | 验证 |
|------|------|------|
| 手写一个最简占位符解析器 | `di/core/PlaceholderResolver.java` | `di.PlaceholderResolverTest` |
| 类型转换的短路与枚举分支 | `di/core/DefaultTypeConverter.java` | `di.DefaultTypeConverterTest` |
| Profile 匹配 | `ioc/core/StandardEnvironment.java` 的 `acceptsProfiles` | `ioc.core.StandardEnvironmentTest` |
| “找不到 Bean”的相似度建议 | `ioc/exception/NoSuchBeanDefinitionException.java` 的 Levenshtein | `ioc.exception.BeansExceptionTest` |
| 新增一个字符串转换器 | `mvc/convert/ConverterRegistry.java` | `mvc.test.ConverterRegistryTest` |

思路：

- 先只写“最简单能通过测试”的版本。例如占位符解析不需要正则，`indexOf(':')` 拆 key/default 就够。
- 断言什么就实现什么：`assertSame` 看引用是否一致，`assertEquals` 看值，`assertThrows` 看异常。不要把需求放大。
- 这些条目本身就是“答案离你很近”的题：卡住时读测试里的示例实现或相邻的 `StringToIntegerConverter`。

## 2. 阶段二：容器与依赖注入（最值得精读的一层）

目的：把 IoC 当一个小系统来理解：**定义 → 实例化 → 注入 → 缓存 → 销毁**。

推荐顺序：

1. `ioc/core/BeanDefinitionRegistry.java`：用 `Map` 维护 `BeanDefinition` 与别名。
2. `ioc/context/ClassPathBeanDefinitionScanner.java` 的 `generateBeanName`：扫描后如何给 Bean 取名。
3. `ioc/core/SingletonCache.java`：三级缓存与循环依赖检测，这是容器最经典的题。
4. `ioc/core/PropertyPlaceholderConfigurer.java`：`${key:default}` 如何解析。
5. `di/core/InjectionEngine.java`：`@Autowired` / `@Resource` / `@Value` / `@Lazy` 的分支。
6. `ioc/core/DefaultListableBeanFactory.java`：按类型找 Bean、别名解析、初始化流程。

思路：

- 三级缓存记三个词即可：`singletonObjects` 是成品，`earlySingletonObjects` 是半成品，`singletonFactories` 是“能造半成品的工厂”。先写 `addSingleton` / `destroySingletons`，再写 `getSingleton` 查找顺序。
- 看 `InjectionEngine` 前先问：字段、setter、构造器三种注入分别是“对谁做事”？答案分别是 `Field`、`Method`、`Constructor`。
- 遇到“为什么容器能解决 A→B→A”这类问题，把调用链写下来：A 创建 → 注入 B → B 创建 → 注入 A → 从三级缓存拿到早期 A → B 完成 → A 完成。
- 运行 `ioc.core.SingletonCacheTest` 和 `di.InjectionEngineTest`，前者测缓存状态，后者测注入结果，两个一起看就能把“状态”和“效果”接起来。

## 3. 阶段三：进入模块实现（AOP → MVC → ORM）

目的：从“容器怎么管理 Bean”进入“Bean 被谁使用”。

推荐顺序：

1. **AOP**：先看 `aop/core/MethodInvocation.java` 的 `proceed()`，这是责任链的心脏；再看 `ProxyFactory#createAopProxy` 如何选 JDK/CGLIB；最后看 `AspectJExpressionPointcut` 怎么把 `execution(...)` 翻译成类/方法匹配。
2. **MVC**：以 `DispatcherServlet#doDispatch` 为入口看整条请求链：`getHandler → getHandlerAdapter → handle → render`；再单独练 `RequestMappingInfo#match` 和 `RequestMappingHandlerAdapter` 的参数绑定。
3. **ORM**：从 SQL 文本到执行结果拆成四段：`GenericTokenParser` 解析 `#{}` → `SqlSourceBuilder` 生成 `BoundSql` → `TypeHandler` 读写参数/结果 → `MapperProxy` 动态代理。

思路：

- 每个模块只记一条主线，其余都是支线。主线分别是：AOP 的“方法调用被谁拦住”，MVC 的“请求怎么找到方法再执行”，ORM 的“SQL 怎么变成 JDBC 执行”。
- 做题前先画一行调用链，例如 MVC：

```text
service()
  -> getHandler(request)              // 找到 HandlerExecutionChain
  -> getHandlerAdapter(handler)       // 找能处理该 handler 的适配器
  -> ha.handle(...)                   // 解析参数、调用方法、处理返回值
  -> render(mv, ...)                  // 解析并渲染 View
```

- 一次只验证一个环节：改参数绑定就只跑 `RequestMappingHandlerAdapterTest`，不要一上来跑全量。

## 4. 阶段四：设计模式练习

进入此阶段的前提：你已经能说出“上一段调用链中谁调用谁”。设计模式不要背定义，直接在这几个已经存在雏形的位置做“提取”练习：

| 模式 | 观察/动手点 | 判断特征 |
|------|------------|----------|
| 工厂方法 | `aop/core/ProxyFactory.java#createAopProxy` | 同一个方法根据条件返回不同代理类 |
| 适配器 | `DispatcherServlet#getHandlerAdapter` + `HandlerAdapter` | 让不同 handler 统一支持 `supports/handle` |
| 策略模式 | `mvc/core/ContentNegotiationManager.java` | 一堆 `if/switch` 在选算法，可抽成接口+注册表 |
| 责任链 | `mvc/core/HandlerExecutionChain.java`、`aop/core/MethodInvocation.java` | 节点依次处理，能“继续推进”也能“中断/终止” |
| 模板方法 | `mvc/handler/RequestMappingHandlerAdapter.java#handle` | 流程固定：解析参数→调用→处理返回值，细节留给子类/钩子 |
| 观察者 | `ioc/event/SimpleApplicationEventMulticaster.java` | 发布者不依赖具体监听器，靠类型索引广播 |
| 建造者 | `di/core/AnnotationInjectEntry.java` | 参数太多、顺序容易传错时引入 Builder |

各模式通用的练习思路：

- 工厂：先不引入抽象，把“选择条件”和“创建动作”分行写清楚，再提取创建逻辑。能跑通后再拆类。
- 策略：先列出所有分支；每个分支就是一个策略；策略接口通常只有一个方法；再写一个注册表或 `Map` 把“条件→策略”收拢。
- 责任链：先找终止条件。`MethodInvocation.proceed()` 的终止条件是 `currentInterceptorIndex` 走到最后一个，否则递归推进。看懂这个再谈重构。
- 模板方法：先找“永远不会变”的骨架，再找“每次可能不同”的细节。骨架写成模板，细节留给可覆写方法。
- 观察者：先找事件类型与监听器类型的对应关系，再实现 `publishEvent`，最后才考虑异步/异常隔离。

## 5. 真正的待补缺口（学到中后期再碰）

这些不是“提取练习”，而是“设计并实现新能力”，难度更大：

- `@Bean` 形式的 `BeanPostProcessor` 时序问题（P0，方案见 `docs/11-P0-BeanBPP-Timing-Fix.md`）。
- 动态 SQL 的 `foreach` / `where` / `set` / `choose` 节点（P2）。
- ORM 一级缓存：`BaseExecutor` 按 `MappedStatement + 参数` 做会话级缓存（P2）。
- 事务事件：把 `TransactionalEventPublisher` 真正接进容器（P2）。

做这类题的正确姿势：先读 `docs/ARCHITECTURE.md` 的缺口说明，再只改一个模块，最后补一条能证明“修复前失败、修复后通过”的测试，不要顺手重构无关代码。

## 6. 卡住时的求助顺序

1. 跑相关测试，把失败断言当成“需求说明书”读。
2. 看同包相邻类：它们已经告诉你数据结构与调用方式。
3. 搜 Spring/MyBatis 同名类：本项目刻意保留同名类与方法，就是为了对照。
4. 一次只解决一个 TODO，删掉它之前先确认对应测试全绿。

最后提醒：练手代码的正确性靠测试判断，不要靠“感觉”。你能让一条 TODO 对应的测试从红变绿，就说明真的写对了。
