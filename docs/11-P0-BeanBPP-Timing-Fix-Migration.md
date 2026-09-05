# P0 修复迁移 / 验证指南：`@Bean` 形式 `BeanPostProcessor` 接入 BPP 链

> 配套设计文档：`docs/11-P0-BeanBPP-Timing-Fix.md`
> 适用版本：修复提交后；迁移方：LightSSM 使用方 / 二次开发者

---

## 1. 背景一句话

此前在 `AnnotationConfigApplicationContext.refresh()` 中，`registerBeanPostProcessors()` 早于 `processBeanMethods()` 运行，导致所有 `@Configuration` 里 `@Bean` 声明的 `BeanPostProcessor`（典型如 `TransactionAutoConfiguration.txBeanPostProcessor()` 返回的 `TransactionalBeanPostProcessor`）在收集 BPP 时尚无 BeanDefinition，永远进不了链。结果：**声明式事务 `@Transactional` 在真实容器内不织入**。

本修复把 `@Bean` 方法的 BeanDefinition 注册提前到 BPP 收集之前（对齐 Spring BFPP 阶段语义），`@Bean` 实例创建仍留在 `preInstantiateSingletons` 阶段（BPP 链就绪后），从而根治且不回退。

---

## 2. 对使用方的影响

- **无破坏性变更**：`@Bean`、`@Configuration`、`@Component` 的既有用法不变。
- **新增能力（正向）**：
  - 用 `@Configuration` + `@Bean BeanPostProcessor` 做扩展，在容器内自动生效。
  - 声明式事务 `@Transactional` 在真实容器内开始生效（此前需 SPI 直登或手动 addBeanPostProcessor）。
- **行为一致性增强**：`@Bean` 声明的 Bean 现在与容器其他 Bean 一样享受完整 BPP 后置处理（AOP 代理、`@Autowired` 等），不再"提前创建、错过 BPP"。

---

## 3. 本地验证步骤

### 3.1 跑全量测试确认不回归

```bash
cd /e/CODE/work/LightSSM
./mvn.sh -s ci-settings.xml -B test
```

预期：`Tests run: 321, Failures: 0, Errors: 0`（或更新后的总数），`BUILD SUCCESS`。

### 3.2 运行新增回归测试（核心证明）

```bash
./mvn.sh -s ci-settings.xml -B test -Dtest=BeanPostProcessorBeanMethodTest
```

该测试构造一个 `@Configuration`，其 `@Bean` 返回一个会在 `postProcessAfterInitialization` 中记录 bean 名的 `BeanPostProcessor`；`refresh()` 后断言记录列表含若干普通 `@Component` 目标。
- 修复前：断言失败（BPP 未被收集）。
- 修复后：断言通过。

### 3.3 声明式事务端到端（建议）

启用 `TransactionAutoConfiguration`（`@Bean` 形式），对任一 `@Transactional` 业务方法调用，在方法体内断言：

```java
assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
```

此前为 `false`，修复后为 `true`。

---

## 4. 代码改动清单（实现阶段落地）

| 文件 | 改动要点 |
|------|----------|
| `AnnotationConfigApplicationContext` | `refresh()` 重排：`registerBeanMethodDefinitions()` 插入到 `registerBeanPostProcessors()` 之前；`processBeanMethods()` 仅负责 `@Configuration` CGLIB 代理；`registerBeanPostProcessors()` 删除/更新既有 TODO |
| `BeanDefinition` | 增加 `configurationBeanName` + `beanMethod` 元数据字段与 getter/setter |
| `DefaultListableBeanFactory` | `createBean`/`doCreateBean` 识别 bean-method 元数据：取配置实例 → 解析参数 → 调用 `@Bean` 方法 → `populateBean` + `initializeBean` |
| `BeanPostProcessorBeanMethodTest`（新增） | 上述回归测试 |

---

## 5. 回滚方案

- 改动集中在 IoC 启动路径，全部位于 `AnnotationConfigApplicationContext.refresh()` 及其辅助方法；回滚即 `git revert` 对应提交。
- 若回滚，声明式事务在容器内将再次不生效——请勿在依赖 `@Transactional` 的生产路径上回滚而不做等价补偿（如保留 SPI 直登 `TransactionalBeanPostProcessor`）。

---

## 6. 验收标准（Definition of Done）

1. `BeanPostProcessorBeanMethodTest` 通过（证明 `@Bean` BPP 进链）。
2. 声明式事务端到端验证通过（`isActualTransactionActive() == true`）。
3. 全量 321 测试仍全绿，无既有行为回退。
4. `docs/ARCHITECTURE.md` §7 与 `README.md` 已知缺口同步将该 P0 标记为已修复。
