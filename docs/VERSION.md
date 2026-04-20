# 版本历史

## v1.0 (ade07b6)
完整版本 - easymybatis ORM框架
- 包含所有模块和文档

---

## v0.8 (7edaf42)
**配置和测试**
- resources: mybatis-config.xml, Mapper XML
- test: 单元测试和集成测试用例

**文档**: docs/v0.8-配置和测试.md
- 配置文件结构详解
- Mapper XML语法
- 测试覆盖范围

---

## v0.7 (dca719a)
**反射、插件和脚本层**
- reflection: MetaClass/MetaObject/Reflector
- plugin: Interceptor/InterceptorChain
- scripting: 动态SQL支持 (if/where/foreach)

**文档**: docs/v0.7-反射插件脚本层.md
- 反射API详解
- 插件拦截机制
- OGNL表达式

---

## v0.6 (00b9984)
**绑定层**
- MapperProxy/MapperMethod
- MapperRegistry/MapperProxyFactory

**文档**: docs/v0.6-绑定层.md
- JDK动态代理原理
- Mapper接口绑定流程
- 多参数处理机制

---

## v0.5 (5abafea)
**映射层和构建器层**
- mapping: BoundSql/MappedStatement/ResultMap
- builder: XMLConfigBuilder/XMLMapperBuilder

**文档**: docs/v0.5-映射和构建器层.md
- MappedStatement结构
- ResultMap配置
- XML解析流程

---

## v0.4 (148e74c)
**执行器层**
- Executor/StatementHandler/ResultSetHandler
- ParameterHandler/KeyGenerator

**文档**: docs/v0.4-执行器层.md
- 查询/更新流程
- 参数设置和结果映射
- 主键生成策略

---

## v0.3 (dbd9163)
**数据源层**
- DataSourceFactory
- UnpooledDataSource/PooledDataSource
- DruidDataSourceFactory

**文档**: docs/v0.3-数据源层.md
- 连接池原理
- PooledConnection代理机制
- Druid集成

---

## v0.2 (cc8f713)
**核心会话模块**
- SqlSession/SqlSessionFactory
- Configuration/SqlSessionFactoryBuilder

**文档**: docs/v0.2-核心会话模块.md
- 工厂模式
- 外观模式
- 线程安全分析

---

## v0.1 (8b76e76)
**基础工具模块**
- annotations: Select/Insert/Update/Delete
- io: Resources
- parsing: GenericTokenParser
- transaction: Transaction/TransactionFactory
- type: 27个类型处理器

**文档**: docs/v0.1-基础工具模块.md
- TypeHandler接口和注册表
- #{} vs ${} 区别
- JDBC类型映射

---

## 学习路径建议

推荐按版本顺序学习：

```
v0.1 (基础) → v0.2 (会话) → v0.3 (数据源)
       ↓
v0.4 (执行) → v0.5 (映射) → v0.6 (绑定)
       ↓
v0.7 (高级) → v0.8 (实战)
```

每个版本都包含：
1. 详细的学习文档 (docs/)
2. 完整的源代码
3. 单元测试