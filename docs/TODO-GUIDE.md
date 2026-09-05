# LightSSM TODO 补充指南

本文约定 LightSSM 代码内 `// TODO` 标记的**统一写法**，并说明如何补充、扫描与清理，避免再次出现“重复壳类 / 误报条目 / 格式不一致”这类历史债。

## 1. 为什么要有规范

当前 `src/main/**` 下有 161 条标准化 `// TODO`（另有测试文件中的练习提示，不进主索引），用于承载两类信息：

1. **手写练习（练习）**：引导读者自己写出某段机制，并给出“写对标志”（可验证的验收条件）。这是项目作为教学框架的核心价值。
2. **优化 / 缺陷 / 特性 / 测试 / 文档**：对已完成代码的改进、待修复问题、待补能力或待补资产。

没有规范的后果已经在仓库里出现：`ioc` 与 `di` 双份注解、`ioc.core` 一票 `@Deprecated` 兼容壳、以及把“注解接口”误标成“重复需要收敛”的失效条目。本指南用于杜绝复发。

## 2. 强制格式

```java
// TODO [等级][分类] 一句话描述；写对标志：可观测的验收条件。
```

- 必须是**单独一行**的 `// TODO`（不是块注释），便于脚本扫描。
- 方括号 `[ ]` 不可省略，中间**无空格**。
- 续行用 `//   文本` 缩进两格，保持在同一方法/字段之上。

### 等级（影响面 / 难度）

| 等级 | 含义 | 例子 |
|------|------|------|
| `L1` | 单方法、局部、可直接动手 | “手写 `Levenshtein` 距离” |
| `L2` | 跨方法、需理解多条调用链 | “抽取 `createConditionContext()` 工厂方法” |
| `L3` | 跨模块 / 架构级 | “把拦截器链改成责任链节点组合” |

### 分类（必填其一）

| 分类 | 适用 | 是否必须带验收标准 |
|------|------|--------------------|
| `练习` | 教学性手写任务 | 必须：`写对标志：` |
| `优化` | 重构 / 模式改进（可带子类如 `优化-策略模式`） | 建议：`验收：` |
| `缺陷` | 明确 bug | 必须：`验收：` |
| `特性` | 待实现能力 | 建议：`验收：` |
| `测试` | 待补单测 | 必须：说明覆盖点 |
| `文档` | 待补文档 | 说明缺什么 |

### 反例 vs 正例

```java
// ❌ 无等级、无分类、无可验证条件
// TODO 这里需要优化一下

// ❌ 老格式（已归一化）：手写练习未加 [练习] 方括号
// TODO [L1] 手写练习: 手写一个自定义 ThreadScope...

// ✅ 规范写法
// TODO [L1][练习] 手写 ThreadScope 实现 Scope 接口（每线程独立实例）；写对标志：同一线程多次 get 拿到同一实例，不同线程拿到不同实例。
```

## 3. 补充一条 TODO 的步骤

1. **定位**：把 TODO 写在**最贴近问题代码的行上方**，最好是它要替换 / 改进的那段实现之上。
2. **定等级与分类**：按上面表格选，不要都标 `L3`。
3. **写验收条件**（练习类用 `写对标志：`，其它用 `验收：`）：
   - 可观测、可断言，例如“`convert("123", Integer.class)` 返回 `Integer.valueOf(123)`”。
   - 避免“应能正常工作”这类不可验证描述。
4. **不要重复已有 TODO**：先 `grep -rn "// TODO" --include=*.java src` 看是否已有同主题条目。
5. **跨方法 / 跨模块的设计类条目**，在描述里点名关键类与方法（如 `DefaultListableBeanFactory#doGetBean`），便于检索。

## 4. 已确立的约束（写入代码即生效）

- **单一来源**：某一机制若已迁移 / 收敛到某包，**旧的兼容壳必须删除**，不要再留 `@Deprecated` 壳类并挂 TODO 说“建议收敛”——收敛动作应直接做掉。
- **注解与容器分离**：注解与注入引擎在 `di` 包，容器本体在 `ioc` 包；新增注解放 `di.annotation`，新增注入能力放 `di.core`，不要往 `ioc.core` 塞第二份。
- **不要误报重复**：`di.annotation.Scope`（注解）与 `ioc.scope.Scope`（接口）**不是重复**，只是同名；标“重复/收敛”前先确认类型与职责是否真的重叠。
- **SPI 与 native-image 同步**：新增会被反射访问的注解 / 类，记得同步更新 `src/main/resources/META-INF/native-image/.../reflect-config.json`。

## 5. 扫描与维护命令

项目提供约定式扫描（`docs/TODO.md` 即由此生成）。在仓库根目录执行：

```bash
# 统计分布
grep -rn "// TODO" --include=*.java src | wc -l

# 按等级
grep -rhoE "// TODO \[L[0-9]\]" --include=*.java src | sort | uniq -c

# 找“练习类但没有验收条件”的条目（应补 写对标志：）
grep -rn "// TODO \[L[0-9]\]\[练习\]" --include=*.java src | grep -v "写对标志"

# 重新生成 docs/TODO.md 索引（扫描 src 下所有 TODO，按 分类/等级/模块 汇总）
python - <<'PY'
# 见本仓库历史脚本：扫描 // TODO [Lx][分类] 标记 → docs/TODO.md
PY
```

> 建议在 CI 或提交前跑一次上面对“练习类缺验收条件”的 grep，作为质量门禁。

## 6. 生命周期

1. **新增**：按第 2、3 节格式写。
2. **解决**：删除该 TODO 行；若涉及代码收敛（如删壳类），一并提交。
3. **复查**：跑 `mvn test` 确保无回归；如改动公共 API，同步 `reflect-config.json`。
4. **索引刷新**：重新生成 `docs/TODO.md`。

## 7. 待办优先级（P0–P3，跨文件）

> 这是项目级的缺口清单，独立于单条 TODO。已在 `docs/ARCHITECTURE.md` §7 详述，此处仅列优先级。

| 优先级 | 主题 |
|--------|------|
| ✅ **P0（已修复）** | AOP 接入容器（`AspectJAutoProxyCreator` 已经 SPI 注册进 BPP 链，`AopContainerWeavingTest` 端到端验证） |
| **P0** | `@Bean` 形式 `BeanPostProcessor` 不进 BPP 链（声明式事务仍受影响，方案见 `docs/11-P0-BeanBPP-Timing-Fix.md`） |
| P1 | 切面解析顺序依赖；Web 作用域双 `ScopeRegistry` 接通；ORM 插件补齐 Executor/Parameter/ResultSet 三处插桩 |
| P2 | 事务事件发布器、ORM 一级缓存、动态 SQL 节点（foreach/where/set/choose） |
| P3 | MVC 组合注解补全 + `@ControllerAdvice` 全局异常处理 |

## 8. 初学练手顺序（建议从这些 TODO 开始）

如果代码基础偏弱，不要按 L1→L3 刷全部清单，按“独立小方法 → 容器机制 → 设计模式重构”三段推进：

> 完整分阶段路线、每个阶段的验证测试与“卡住时的求助顺序”见 [`docs/PRACTICE.md`](PRACTICE.md)。

1. **独立小方法（先建立“能跑通测试”的正反馈）**
   - `di/core/PlaceholderResolver.java`：自己实现一个 `Map` 版解析器，跑 `PlaceholderResolverTest`。
   - `di/core/DefaultTypeConverter.java` 中“已是指定类型直接短路”和“枚举解析”两个分支。
   - `ioc/core/StandardEnvironment.java` 的 `acceptsProfiles`；`ioc/exception/NoSuchBeanDefinitionException.java` 的 Levenshtein 距离。
   - `mvc/convert/ConverterRegistry.java`：仿照现有 `StringToIntegerConverter` 新增一个转换器。
2. **容器机制（理解“一个方法在框架里何时被谁调用”）**
   - `ioc/core/SingletonCache.java`：`addSingleton` / `destroySingletons` / `getSingleton`，对照 `SingletonCacheTest` 理解三级缓存。
   - `ioc/core/PropertyPlaceholderConfigurer.java` 的 `${key:default}` 解析；`ioc/context/ClassPathBeanDefinitionScanner.java` 的 `generateBeanName`。
   - `mvc/handler/RequestMappingInfo.java`：给 `{var}` 补可选变量/带约束变量，跑 `RequestMappingInfoTest`。
3. **设计模式（只做“当前实现已经能看出雏形”的几处）**
   - 工厂方法：`aop/core/ProxyFactory.java#createAopProxy`（按条件返回 JDK/CGLIB 代理）。
   - 适配器：`mvc/servlet/DispatcherServlet.java#getHandlerAdapter`（`supports/handle` 已是适配器雏形）。
   - 模板方法：`mvc/handler/RequestMappingHandlerAdapter.java#handle`（参数解析→调用→返回处理的三步骨架）。
   - 策略模式：`mvc/core/ContentNegotiationManager.java`（把 `format` 的 switch 拆成可替换策略）。
   - 责任链：`mvc/core/HandlerExecutionChain.java` 与 `aop/core/MethodInvocation.java#proceed`（先看懂“推进+终止”，再考虑重构）。

每完成一条：删掉该行 `// TODO`，跑对应包的单测；全绿后再进入下一条。若想做但暂时卡住，先读同文件相邻实现，再对照 Spring/MyBatis 同名类。
