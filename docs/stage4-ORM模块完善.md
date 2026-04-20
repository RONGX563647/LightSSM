# 第4阶段：ORM模块完善

## 变动内容

### 1. 从mybatis/easymybatis复制ORM代码
将原有的ORM模块代码复制到新项目，保持功能完整性。

### 2. 核心模块结构

#### annotations包 - 注解定义
- `@Select` - 查询注解
- `@Insert` - 插入注解
- `@Update` - 更新注解
- `@Delete` - 删除注解

#### binding包 - Mapper代理机制
- `MapperProxy` - JDK动态代理实现
- `MapperProxyFactory` - Mapper代理工厂
- `MapperMethod` - Mapper方法封装
- `MapperRegistry` - Mapper注册中心

#### builder包 - 配置构建器
- `BaseBuilder` - 基础构建器
- `XMLConfigBuilder` - XML配置构建器
- `XMLMapperBuilder` - XML Mapper构建器
- `XMLStatementBuilder` - XML语句构建器
- `SqlSourceBuilder` - SQL源构建器

#### datasource包 - 数据源实现
- `DataSourceFactory` - 数据源工厂接口
- `UnpooledDataSource` - 无连接池数据源
- `PooledDataSource` - 自定义连接池
- `DruidDataSourceFactory` - Druid连接池集成

#### executor包 - SQL执行器
- `Executor` - 执行器接口
- `BaseExecutor` - 基础执行器（模板方法）
- `SimpleExecutor` - 简单执行器
- `StatementHandler` - 语句处理器
- `ParameterHandler` - 参数处理器
- `ResultSetHandler` - 结果集处理器

#### mapping包 - SQL映射配置
- `MappedStatement` - 映射语句
- `BoundSql` - 绑定SQL
- `Environment` - 环境配置
- `ResultMap` - 结果映射
- `ParameterMapping` - 参数映射

#### parsing包 - SQL解析
- `GenericTokenParser` - 通用Token解析器
- `TokenHandler` - Token处理器

#### plugin包 - 插件机制
- `Interceptor` - 拦截器接口
- `InterceptorChain` - 拦截器链
- `Plugin` - 插件代理
- `Invocation` - 方法调用封装

#### reflection包 - 反射工具
- `Reflector` - 反射器
- `MetaObject` - 元对象
- `MetaClass` - 元类
- `ObjectFactory` - 对象工厂

#### scripting包 - 动态SQL
- `LanguageDriver` - 语言驱动接口
- `XMLLanguageDriver` - XML语言驱动
- `DynamicSqlSource` - 动态SQL源
- `SqlNode` - SQL节点接口
- `IfSqlNode` - if节点
- `TrimSqlNode` - trim节点
- `MixedSqlNode` - 混合节点
- `OgnlCache` - OGNL缓存

#### session包 - 会话管理
- `SqlSession` - 会话接口
- `SqlSessionFactory` - 会话工厂
- `SqlSessionFactoryBuilder` - 会话工厂构建器
- `Configuration` - 全局配置
- `DefaultSqlSession` - 默认会话实现
- `DefaultSqlSessionFactory` - 默认会话工厂

#### transaction包 - 事务管理
- `Transaction` - 事务接口
- `JdbcTransaction` - JDBC事务实现
- `TransactionFactory` - 事务工厂

#### type包 - 类型处理器
- `TypeHandler` - 类型处理器接口
- `BaseTypeHandler` - 基础类型处理器
- `StringTypeHandler` - String类型处理器
- `IntegerTypeHandler` - Integer类型处理器
- `LongTypeHandler` - Long类型处理器
- `DateTypeHandler` - Date类型处理器
- 等30+类型处理器

### 3. 资源文件
- `mybatis-config-datasource.xml` - MyBatis配置示例
- `Activity_Mapper.xml` - Mapper XML示例

### 4. 测试代码
- `MapperProxyFactoryTest` - Mapper代理工厂测试
- `MapperProxyTest` - Mapper代理测试
- `ParameterExpressionTest` - 参数表达式测试
- `GenericTokenParserTest` - Token解析器测试
- `InterceptorChainTest` - 拦截器链测试
- `PluginTest` - 插件测试
- `ReflectorTest` - 反射器测试
- `TypeHandlerRegistryTest` - 类型处理器注册测试
- `ApiTest` - API集成测试

## 设计说明

### ORM核心架构

```
SqlSessionFactoryBuilder
    ↓ 解析XML配置
Configuration (全局配置中心)
    ↓
SqlSessionFactory
    ↓ openSession()
SqlSession (会话入口)
    ↓ getMapper()
MapperProxy (JDK动态代理)
    ↓ invoke()
MapperMethod.execute()
    ↓
Executor (执行器)
    ↓ BaseExecutor模板方法
SimpleExecutor.doQuery()
    ↓
StatementHandler (语句处理器)
    ↓ prepare()、parameterize()
ParameterHandler (参数绑定)
    ↓
ResultSetHandler (结果映射)
    ↓
POJO对象
```

### 动态SQL实现

**OGNL表达式**：
```xml
<select id="selectUser" resultType="User">
    SELECT * FROM user
    <trim prefix="WHERE" prefixOverrides="AND | OR">
        <if test="name != null">
            AND name = #{name}
        </if>
        <if test="phone != null">
            AND phone = #{phone}
        </if>
    </trim>
</select>
```

**处理流程**：
1. XMLScriptBuilder解析XML，构建SqlNode树
2. DynamicSqlSource.getBoundSql()执行动态SQL
3. OgnlCache.getValue()计算OGNL表达式
4. IfSqlNode.apply()判断条件是否成立
5. TrimSqlNode.apply()处理前缀后缀
6. 生成最终SQL语句

### 插件拦截机制

**拦截器链**：
```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class})
})
public class TestPlugin implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 前置处理
        Object result = invocation.proceed();
        // 后置处理
        return result;
    }
}
```

**代理流程**：
1. Configuration.newStatementHandler()创建处理器
2. InterceptorChain.pluginAll()应用所有插件
3. Plugin.wrap()创建代理对象
4. 方法调用时触发拦截器链

## 文件清单
- src/main/java/com/lightframework/orm/**/*.java（约150个文件）
- src/main/resources/mapper/*.xml
- src/main/resources/mybatis-config-datasource.xml
- src/test/java/com/lightframework/orm/**/*.java
- docs/stage4-ORM模块完善.md

## 下一步计划
第5阶段将实现AOP切面功能，包括：
- @Aspect/@Before/@After/@Around注解
- JDK/CGLIB双代理机制
- AspectJ表达式切入点