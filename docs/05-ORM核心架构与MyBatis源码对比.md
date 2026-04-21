# 05 - ORM核心架构与MyBatis源码对比

## 1. MyBatis架构概述

MyBatis是一款优秀的持久层框架，它支持自定义 SQL、存储过程以及高级映射。

### 1.1 核心架构层次

```
应用层
    ↓
API接口层（SqlSession）
    ↓
数据处理层（Executor）
    ↓
SQL解析层（StatementHandler）
    ↓
JDBC支撑层（Connection/Statement/ResultSet）
    ↓
数据库
```

### 1.2 核心组件

| 组件 | 职责 |
|-----|------|
| SqlSession | 数据库会话接口 |
| Executor | SQL执行器 |
| StatementHandler | SQL语句处理器 |
| ParameterHandler | 参数处理器 |
| ResultSetHandler | 结果集处理器 |
| Configuration | 全局配置 |
| MappedStatement | 映射语句 |

## 2. SqlSession接口设计

### 2.1 接口定义

**LightSSM实现**（[SqlSession.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/session/SqlSession.java)）：

```java
public interface SqlSession extends Closeable {
    <T> T selectOne(String statement);
    <T> T selectOne(String statement, Object parameter);
    <E> List<E> selectList(String statement, Object parameter);
    int insert(String statement, Object parameter);
    int update(String statement, Object parameter);
    Object delete(String statement, Object parameter);
    void commit();
    Configuration getConfiguration();
    <T> T getMapper(Class<T> type);
}
```

### 2.2 与MyBatis官方对比

**MyBatis官方SqlSession接口**：

```java
public interface SqlSession extends Closeable {
    // 查询方法
    <T> T selectOne(String statement);
    <T> T selectOne(String statement, Object parameter);
    <E> List<E> selectList(String statement);
    <E> List<E> selectList(String statement, Object parameter);
    <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);
    <K, V> Map<K, V> selectMap(String statement, String mapKey);
    <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);
    <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);
    <T> Cursor<T> selectCursor(String statement);
    <T> Cursor<T> selectCursor(String statement, Object parameter);
    <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);
    
    // 增删改方法
    int insert(String statement);
    int insert(String statement, Object parameter);
    int update(String statement);
    int update(String statement, Object parameter);
    int delete(String statement);
    int delete(String statement, Object parameter);
    
    // 事务方法
    void commit();
    void commit(boolean forceCommit);
    void rollback();
    void rollback(boolean forceRollback);
    
    // 其他方法
    void flushStatements();
    void clearCache();
    Configuration getConfiguration();
    <T> T getMapper(Class<T> type);
    boolean isConnectionOwned();
    Connection getConnection();
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| 查询方法 | selectOne/selectList/selectMap/selectCursor | selectOne/selectList |
| 分页支持 | RowBounds参数 | 不支持 |
| Cursor流式查询 | 支持 | 不支持 |
| 增删改方法 | 完整支持 | 支持 |
| 事务方法 | commit/rollback（带force选项） | 仅commit |
| 缓存管理 | clearCache | 不支持 |
| 连接管理 | getConnection/isConnectionOwned | 不支持 |

## 3. DefaultSqlSession实现

### 3.1 核心实现

**LightSSM实现**（[DefaultSqlSession.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/session/defaults/DefaultSqlSession.java)）：

```java
public class DefaultSqlSession implements SqlSession {
    private Configuration configuration;
    private Executor executor;
    
    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        logger.info("执行查询 statement：{} parameter：{}", statement, JSON.toJSONString(parameter));
        MappedStatement ms = configuration.getMappedStatement(statement);
        try {
            return executor.query(ms, parameter, RowBounds.DEFAULT, 
                Executor.NO_RESULT_HANDLER, ms.getSqlSource().getBoundSql(parameter));
        } catch (SQLException e) {
            throw new RuntimeException("Error querying database.  Cause: " + e);
        }
    }
    
    @Override
    public int update(String statement, Object parameter) {
        MappedStatement ms = configuration.getMappedStatement(statement);
        try {
            return executor.update(ms, parameter);
        } catch (SQLException e) {
            throw new RuntimeException("Error updating database.  Cause: " + e);
        }
    }
    
    @Override
    public <T> T getMapper(Class<T> type) {
        return configuration.getMapper(type, this);
    }
}
```

### 3.2 与MyBatis官方对比

**MyBatis官方的DefaultSqlSession**核心逻辑：

```java
public class DefaultSqlSession implements SqlSession {
    private final Configuration configuration;
    private final Executor executor;
    
    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.query(ms, wrapCollection(parameter), rowBounds, 
                Executor.NO_RESULT_HANDLER);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.", e);
        } finally {
            ErrorContext.instance().reset();
        }
    }
    
    @Override
    public int update(String statement, Object parameter) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.update(ms, wrapCollection(parameter));
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error updating database.", e);
        } finally {
            ErrorContext.instance().reset();
        }
    }
    
    // 参数包装：处理集合和数组参数
    private Object wrapCollection(final Object object) {
        if (object instanceof List) {
            StrictMap<Object> map = new StrictMap<>();
            map.put("list", object);
            return map;
        } else if (object != null && object.getClass().isArray()) {
            StrictMap<Object> map = new StrictMap<>();
            map.put("array", object);
            return map;
        }
        return object;
    }
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| 参数包装 | wrapCollection处理集合/数组 | 不支持 |
| 异常处理 | ExceptionFactory统一包装 | 直接包装为RuntimeException |
| ErrorContext | 上下文重置 | 不支持 |
| 日志 | 使用JDK Logger | 使用SLF4J |

## 4. Executor执行器

### 4.1 Executor接口

**LightSSM定义**：
```java
public interface Executor {
    ResultHandler NO_RESULT_HANDLER = null;
    
    int update(MappedStatement ms, Object parameter) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, 
        ResultHandler resultHandler, BoundSql boundSql) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, 
        ResultHandler resultHandler) throws SQLException;
    
    Transaction getTransaction();
    void commit(boolean required) throws SQLException;
    void rollback(boolean required) throws SQLException;
    void close(boolean forceRollback);
}
```

**MyBatis官方Executor接口**：

```java
public interface Executor {
    int update(MappedStatement ms, Object parameter) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, 
        ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, 
        ResultHandler resultHandler) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;
    <E> List<E> query(MappedStatement ms, Object parameter) throws SQLException;
    
    void deferLoad(MappedStatement ms, MetaObject resultObject, String property, 
        CacheKey key, Class<?> targetType);
    CacheKey createCacheKey(MappedStatement ms, Object parameterObject, 
        RowBounds rowBounds, BoundSql boundSql);
    boolean isCached(MappedStatement ms, CacheKey key);
    void clearLocalCache();
    void flushStatements();
    
    void commit(boolean required) throws SQLException;
    void rollback(boolean required) throws SQLException;
    void close(boolean forceRollback);
    boolean isClosed();
    void setExecutorWrapper(Executor executor);
    void deferLoad(MappedStatement ms, MetaObject resultObject, String property, 
        CacheKey key, Class<?> targetType);
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| 查询方法 | 4个重载方法（含CacheKey） | 2个重载方法 |
| 缓存支持 | createCacheKey/isCached/clearLocalCache | 不支持 |
| 延迟加载 | deferLoad | 不支持 |
| 批量操作 | flushStatements | 不支持 |
| 状态检查 | isClosed | 不支持 |

### 4.2 BaseExecutor模板基类

**LightSSM实现**（[BaseExecutor.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/executor/BaseExecutor.java)）：

```java
public abstract class BaseExecutor implements Executor {
    protected Configuration configuration;
    protected Transaction transaction;
    protected Executor wrapper;
    private boolean closed;
    
    @Override
    public int update(MappedStatement ms, Object parameter) throws SQLException {
        return doUpdate(ms, parameter);
    }
    
    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, 
        RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (closed) {
            throw new RuntimeException("Executor was closed.");
        }
        return doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
    }
    
    protected abstract int doUpdate(MappedStatement ms, Object parameter) throws SQLException;
    protected abstract <E> List<E> doQuery(MappedStatement ms, Object parameter, 
        RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException;
}
```

**MyBatis官方的BaseExecutor**包含：
- 一级缓存（PerpetualCache）
- 延迟加载（DeferredLoad）
- 查询缓存（queryFromDatabase）
- 事务日志（Transaction）

### 4.3 SimpleExecutor简单执行器

**LightSSM实现**（[SimpleExecutor.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/executor/SimpleExecutor.java)）：

```java
public class SimpleExecutor extends BaseExecutor {
    @Override
    protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(
                this, ms, parameter, RowBounds.DEFAULT, null, null);
            stmt = prepareStatement(handler);
            return handler.update(stmt);
        } finally {
            closeStatement(stmt);
        }
    }
    
    @Override
    protected <E> List<E> doQuery(MappedStatement ms, Object parameter, 
        RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        Statement stmt = null;
        try {
            Configuration configuration = ms.getConfiguration();
            StatementHandler handler = configuration.newStatementHandler(
                wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
            stmt = prepareStatement(handler);
            return handler.query(stmt, resultHandler);
        } finally {
            closeStatement(stmt);
        }
    }
    
    private Statement prepareStatement(StatementHandler handler) throws SQLException {
        Statement stmt;
        Connection connection = transaction.getConnection();
        stmt = handler.prepare(connection);
        handler.parameterize(stmt);
        return stmt;
    }
}
```

**MyBatis官方Executor层次**：

```
Executor（接口）
├── BaseExecutor（抽象基类）
│   ├── SimpleExecutor（简单执行器）
│   ├── ReuseExecutor（可重用执行器）
│   └── BatchExecutor（批量执行器）
└── CachingExecutor（缓存执行器）
```

**对比分析**：

| 执行器类型 | MyBatis官方 | LightSSM |
|-----------|-----------|----------|
| SimpleExecutor | 每次创建新Statement | 每次创建新Statement |
| ReuseExecutor | 重用Statement | 不支持 |
| BatchExecutor | 批量操作 | 不支持 |
| CachingExecutor | 二级缓存 | 不支持 |

## 5. Mapper代理机制

### 5.1 MapperRegistry注册中心

**LightSSM实现**（[MapperRegistry.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/binding/MapperRegistry.java)）：

```java
public class MapperRegistry {
    private Configuration config;
    private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();
    
    public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
        final MapperProxyFactory<T> mapperProxyFactory = 
            (MapperProxyFactory<T>) knownMappers.get(type);
        if (mapperProxyFactory == null) {
            throw new RuntimeException("Type " + type + " is not known to the MapperRegistry.");
        }
        try {
            return mapperProxyFactory.newInstance(sqlSession);
        } catch (Exception e) {
            throw new RuntimeException("Error getting mapper instance. Cause: " + e, e);
        }
    }
    
    public <T> void addMapper(Class<T> type) {
        if (type.isInterface()) {
            if (hasMapper(type)) {
                throw new RuntimeException("Type " + type + " is already known to the MapperRegistry.");
            }
            knownMappers.put(type, new MapperProxyFactory<>(type));
            
            // 解析注解类语句配置
            MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
            parser.parse();
        }
    }
}
```

### 5.2 MapperProxy代理

**LightSSM实现**（[MapperProxy.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/binding/MapperProxy.java)）：

```java
public class MapperProxy<T> implements InvocationHandler, Serializable {
    private SqlSession sqlSession;
    private final Class<T> mapperInterface;
    private final Map<Method, MapperMethod> methodCache;
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else {
            final MapperMethod mapperMethod = cachedMapperMethod(method);
            return mapperMethod.execute(sqlSession, args);
        }
    }
    
    private MapperMethod cachedMapperMethod(Method method) {
        MapperMethod mapperMethod = methodCache.get(method);
        if (mapperMethod == null) {
            mapperMethod = new MapperMethod(mapperInterface, method, sqlSession.getConfiguration());
            methodCache.put(method, mapperMethod);
        }
        return mapperMethod;
    }
}
```

### 5.3 与MyBatis官方对比

**MyBatis官方的MapperProxy.invoke()**：

```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        } else if (isDefaultMethod(method)) {
            return invokeDefaultMethod(proxy, method, args);
        }
    } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
    }
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    return mapperMethod.execute(sqlSession, args);
}

// Java 8默认方法支持
private Object invokeDefaultMethod(Object proxy, Method method, Object[] args) throws Throwable {
    final Class<?> declaringClass = method.getDeclaringClass();
    return MethodHandles.lookup()
        .findSpecial(declaringClass, method.getName(), 
            MethodType.methodType(method.getReturnType(), method.getParameterTypes()),
            declaringClass)
        .bindTo(proxy)
        .invokeWithArguments(args);
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| Object方法处理 | 支持（equals/hashCode/toString） | 支持 |
| Java 8默认方法 | 支持（MethodHandles） | 不支持 |
| 方法缓存 | ConcurrentHashMap | HashMap |
| 异常处理 | ExceptionUtil.unwrapThrowable | 直接抛出 |

## 6. Configuration配置中心

### 6.1 Configuration核心属性

**LightSSM实现**（[Configuration.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/orm/session/Configuration.java)）：

```java
public class Configuration {
    protected Environment environment;
    protected boolean useGeneratedKeys = false;
    
    // 注册机
    protected MapperRegistry mapperRegistry = new MapperRegistry(this);
    protected final Map<String, MappedStatement> mappedStatements = new HashMap<>();
    protected final Map<String, ResultMap> resultMaps = new HashMap<>();
    protected final Map<String, KeyGenerator> keyGenerators = new HashMap<>();
    
    // 插件链
    protected final InterceptorChain interceptorChain = new InterceptorChain();
    
    // 类型处理
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
    protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
    protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();
    
    // 对象工厂
    protected ObjectFactory objectFactory = new DefaultObjectFactory();
    protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
    
    protected final Set<String> loadedResources = new HashSet<>();
    protected String databaseId;
}
```

### 6.2 与MyBatis官方对比

**MyBatis官方Configuration**包含500+个属性和方法：

```java
public class Configuration {
    // 环境配置
    protected Environment environment;
    protected boolean useGeneratedKeys;
    protected boolean useActualParamName = true;
    protected boolean safeRowBoundsEnabled = false;
    protected boolean safeResultHandlerEnabled = true;
    protected boolean mapUnderscoreToCamelCase = false;
    protected boolean aggressiveLazyLoading = false;
    // ... 100+个配置项
    
    // 注册机
    protected final MapperRegistry mapperRegistry;
    protected final Map<String, MappedStatement> mappedStatements;
    protected final Map<String, ResultMap> resultMaps;
    protected final Map<String, Cache> caches;
    // ... 20+个Map
    
    // 工厂
    protected ObjectFactory objectFactory;
    protected ObjectWrapperFactory objectWrapperFactory;
    protected ReflectorFactory reflectorFactory;
    // ... 10+个工厂
    
    // 其他配置
    protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
    protected Integer defaultFetchSize = null;
    protected Integer defaultStatementTimeout = null;
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
    // ... 50+个配置
}
```

**对比分析**：

| 配置项 | MyBatis官方 | LightSSM |
|-------|-----------|----------|
| 基础配置 | 完整支持 | 基础支持 |
| 缓存配置 | Cache/CacheManager | 不支持 |
| 懒加载 | aggressiveLazyLoading等 | 不支持 |
| 自动映射 | mapUnderscoreToCamelCase | 不支持 |
| 默认配置 | fetchSize/timeout/executorType | 不支持 |
| 反射工厂 | ReflectorFactory | 不支持 |

## 7. 设计模式总结

### 7.1 工厂模式

```java
// Configuration作为工厂
public class Configuration {
    public Executor newExecutor(Transaction transaction) {
        return new SimpleExecutor(this, transaction);
    }
    
    public StatementHandler newStatementHandler(...) {
        return new PreparedStatementHandler(...);
    }
    
    public ResultSetHandler newResultSetHandler(...) {
        return new DefaultResultSetHandler(...);
    }
}
```

### 7.2 代理模式

```java
// Mapper接口代理
public class MapperProxy<T> implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }
}
```

### 7.3 模板方法模式

```java
// BaseExecutor定义模板
public abstract class BaseExecutor implements Executor {
    public int update(MappedStatement ms, Object parameter) {
        return doUpdate(ms, parameter); // 抽象方法
    }
    
    protected abstract int doUpdate(MappedStatement ms, Object parameter);
    protected abstract <E> List<E> doQuery(...);
}
```

### 7.4 注册表模式

```java
// 各种注册表
public class Configuration {
    protected final MapperRegistry mapperRegistry;
    protected final Map<String, MappedStatement> mappedStatements;
    protected final TypeAliasRegistry typeAliasRegistry;
    protected final TypeHandlerRegistry typeHandlerRegistry;
}
```

## 8. 面试考点

### 8.1 高频问题

1. **MyBatis的执行流程是什么？**
   - SqlSession接收请求
   - Executor执行SQL
   - StatementHandler处理语句
   - ResultSetHandler处理结果

2. **Mapper接口为什么不需要实现类？**
   - JDK动态代理
   - MapperProxy拦截方法调用
   - 根据方法名查找MappedStatement

3. **#{}和${}的区别？**
   - #{}：预编译，防止SQL注入
   - ${}：字符串替换，有SQL注入风险

### 8.2 深度问题

1. **MyBatis的一级缓存和二级缓存？**
   - 一级缓存：SqlSession级别，默认开启
   - 二级缓存：Namespace级别，需要配置

2. **MyBatis的插件原理？**
   - 拦截四大对象（Executor/StatementHandler/ParameterHandler/ResultSetHandler）
   - 责任链模式
   - 动态代理

## 9. 总结

LightSSM的ORM模块完整实现了MyBatis的核心机制：

- **SqlSession**：数据库会话接口
- **Executor**：SQL执行器（SimpleExecutor）
- **Mapper代理**：JDK动态代理
- **Configuration**：配置中心
- **StatementHandler**：语句处理器

通过对照MyBatis官方源码，可以看到LightSSM保留了ORM的核心流程，剥离了高级特性（如缓存、懒加载、批量操作等），非常适合学习理解MyBatis原理。

---

**上一篇**：[04 - MVC框架实现与Spring MVC源码对比](04-MVC框架实现与SpringMVC源码对比.md)

**下一篇**：[06 - 数据源与事务管理实现](06-数据源与事务管理实现.md)
