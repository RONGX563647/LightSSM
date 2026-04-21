# 07 - SQL解析与动态SQL实现

## 1. SQL解析架构

### 1.1 SqlSource接口

**LightSSM实现**：
```java
public interface SqlSource {
    BoundSql getBoundSql(Object parameterObject);
}
```

**MyBatis官方SqlSource**：
```java
public interface SqlSource {
    BoundSql getBoundSql(Object parameterObject);
}
```

**接口完全一致**。

### 1.2 SqlSource层次结构

```
SqlSource（接口）
├── StaticSqlSource（静态SQL）
└── DynamicSqlSource（动态SQL）
```

## 2. 静态SQL解析

### 2.1 StaticSqlSource

**LightSSM实现**：
```java
public class StaticSqlSource implements SqlSource {
    private String sql;
    private List<ParameterMapping> parameterMappings;
    
    public StaticSqlSource(String sql) {
        this.sql = sql;
        this.parameterMappings = new ArrayList<>();
    }
    
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return new BoundSql(sql, parameterMappings, parameterObject);
    }
}
```

### 2.2 与MyBatis官方对比

**MyBatis官方的StaticSqlSource**：
- 支持多个SQL片段
- 支持参数映射列表
- 支持额外的参数对象

**LightSSM简化**：
- 单一SQL字符串
- 基础参数映射

## 3. 动态SQL解析

### 3.1 DynamicSqlSource

**LightSSM实现**：
```java
public class DynamicSqlSource implements SqlSource {
    private Configuration configuration;
    private SqlNode rootSqlNode;
    
    public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
        this.configuration = configuration;
        this.rootSqlNode = rootSqlNode;
    }
    
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        // 创建动态上下文
        DynamicContext context = new DynamicContext(configuration, parameterObject);
        
        // 执行SQL节点树
        rootSqlNode.apply(context);
        
        // 获取生成的SQL
        String sql = context.getSql();
        
        // 创建BoundSql
        return new BoundSql(sql, null, parameterObject);
    }
}
```

### 3.2 与MyBatis官方对比

**MyBatis官方的DynamicSqlSource**：
```java
public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    rootSqlNode.apply(context);
    SqlSource sqlSource = new StaticSqlSource(configuration, context.getSql(), 
        context.getBindings());
    return sqlSource.getBoundSql(parameterObject);
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| 动态上下文 | DynamicContext | DynamicContext |
| SQL节点树 | SqlNode.apply() | SqlNode.apply() |
| 参数绑定 | 完整的Bindings处理 | 简化处理 |
| 二次解析 | StaticSqlSource再次解析 | 直接创建BoundSql |

## 4. SQL节点体系

### 4.1 SqlNode接口

**LightSSM定义**：
```java
public interface SqlNode {
    boolean apply(DynamicContext context);
}
```

**MyBatis官方SqlNode**：
```java
public interface SqlNode {
    boolean apply(DynamicContext context);
}
```

**接口完全一致**。

### 4.2 SqlNode实现类

```
SqlNode（接口）
├── StaticTextSqlNode（静态文本）
├── DynamicContext（动态内容）
├── IfSqlNode（IF条件）
├── TrimSqlNode（TRIM修剪）
├── WhereSqlNode（WHERE子句）
├── SetSqlNode（SET子句）
├── ForEachSqlNode（循环）
└── MixedSqlNode（混合节点）
```

### 4.3 IfSqlNode条件节点

**LightSSM实现**：
```java
public class IfSqlNode implements SqlNode {
    private SqlNode contents;
    private String test;
    
    @Override
    public boolean apply(DynamicContext context) {
        // 绑定根对象
        context.bind("rootObject", context.getOriginalParameterObject());
        
        // 绑定参数对象
        context.bind("_parameter", context.getOriginalParameterObject());
        
        // 解析表达式
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        if (evaluator.evaluateBoolean(test, context)) {
            return contents.apply(context);
        }
        return true;
    }
}
```

### 4.4 与MyBatis官方对比

**MyBatis官方的IfSqlNode**：
```java
public boolean apply(DynamicContext context) {
    // 使用OGNL表达式引擎
    if (evaluator.evaluateBoolean(test, context.getBindings())) {
        contents.apply(context);
        return true;
    }
    return true;
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| 表达式引擎 | OGNL | OGNL |
| 绑定对象 | 完整的Bindings | 简化绑定 |
| 表达式缓存 | ExpressionCache | 不支持 |

### 4.5 ForEachSqlNode循环节点

**LightSSM实现**：
```java
public class ForEachSqlNode implements SqlNode {
    private String collection;
    private String item;
    private String index;
    private String open;
    private String close;
    private String separator;
    private SqlNode contents;
    
    @Override
    public boolean apply(DynamicContext context) {
        // 获取集合对象
        Object collectionObject = getCollectionObject(context);
        
        // 处理数组或集合
        if (collectionObject instanceof Iterable) {
            Iterable<?> iterable = (Iterable<?>) collectionObject;
            processIterable(iterable, context);
        } else if (collectionObject.getClass().isArray()) {
            processArray(collectionObject, context);
        } else if (collectionObject instanceof Map) {
            processMap((Map<?, ?>) collectionObject, context);
        }
        
        return true;
    }
    
    private void processIterable(Iterable<?> iterable, DynamicContext context) {
        int i = 0;
        for (Object itemObject : iterable) {
            applyPrefix(context, i);
            
            // 绑定循环变量
            context.bind(item, itemObject);
            if (index != null) {
                context.bind(index, i);
            }
            
            // 执行内容
            contents.apply(context);
            
            applySuffix(context, i);
            i++;
        }
    }
}
```

### 4.6 与MyBatis官方对比

**MyBatis官方的ForEachSqlNode**：
- 支持Iterable、数组、Map、Iterator
- 支持可索引的对象
- 完整的前缀/后缀/分隔符处理
- 支持去除最后一个分隔符

**LightSSM简化**：
- 支持基础的Iterable和数组
- 基础的前缀/后缀处理
- 不支持Iterator和Map的完整处理

## 5. OGNL表达式解析

### 5.1 ExpressionEvaluator

**LightSSM实现**：
```java
public class ExpressionEvaluator {
    
    public boolean evaluateBoolean(String expression, Object parameterObject) {
        try {
            OgnlContext context = new OgnlContext(null, null, new OgnlMemberAccess());
            Object value = Ognl.getValue(expression, context, parameterObject);
            
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0.0;
            } else {
                return value != null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating expression: " + expression, e);
        }
    }
    
    public Object evaluate(String expression, Object parameterObject) {
        try {
            OgnlContext context = new OgnlContext(null, null, new OgnlMemberAccess());
            return Ognl.getValue(expression, context, parameterObject);
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating expression: " + expression, e);
        }
    }
}
```

### 5.2 与MyBatis官方对比

**MyBatis官方的ExpressionEvaluator**：
```java
public class ExpressionEvaluator {
    public boolean evaluateBoolean(String expression, Object parameterObject) {
        try {
            Object value = OgnlCache.getValue(expression, parameterObject);
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof Number) {
                return ((Number) value).doubleValue() != 0.0;
            } else {
                return value != null;
            }
        } catch (OgnlException e) {
            throw new OgnlException("Error evaluating expression: " + expression, e);
        }
    }
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| OGNL缓存 | OgnlCache | 直接调用Ognl |
| 异常处理 | OgnlException | RuntimeException |
| 类型转换 | 完整的类型转换 | 基础类型转换 |

## 6. 动态上下文

### 6.1 DynamicContext

**LightSSM实现**：
```java
public class DynamicContext {
    private final ContextMap bindings;
    private final StringBuilder sql = new StringBuilder();
    
    public DynamicContext(Configuration configuration, Object parameterObject) {
        this.bindings = new ContextMap();
        
        if (parameterObject != null) {
            if (parameterObject instanceof Map) {
                bindings.putAll((Map) parameterObject);
            } else {
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                bindings.put("parameterObject", parameterObject);
                // 绑定对象属性
                for (String propertyName : metaObject.getGetterNames()) {
                    bindings.put(propertyName, metaObject.getValue(propertyName));
                }
            }
        }
    }
    
    public void bind(String key, Object value) {
        bindings.put(key, value);
    }
    
    public void appendSql(String sql) {
        this.sql.append(sql);
        this.sql.append(" ");
    }
    
    public String getSql() {
        return this.sql.toString().trim();
    }
    
    public Object get(String key) {
        return bindings.get(key);
    }
    
    private static class ContextMap extends HashMap<String, Object> {
        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            if (value == null && key != null) {
                // 尝试使用MetaObject获取
                Object parameterObject = get("parameterObject");
                if (parameterObject != null) {
                    MetaObject metaObject = MetaObject.forObject(parameterObject);
                    value = metaObject.getValue(key.toString());
                }
            }
            return value;
        }
    }
}
```

### 6.2 与MyBatis官方对比

**MyBatis官方的DynamicContext**：
```java
public class DynamicContext {
    private final ContextMap bindings;
    private final GenericTokenParser parser;
    private final StringBuilder sql = new StringBuilder();
    
    public DynamicContext(Configuration configuration, Object parameterObject) {
        this.bindings = new ContextMap(configuration, parameterObject);
        this.parser = new TokenParser(new TokenHandler() {
            public String handleToken(String content) {
                Object parameter = bindings.get(content);
                return parameter == null ? "" : String.valueOf(parameter);
            }
        });
    }
    
    public void appendSql(String sql) {
        this.sql.append(sql);
        this.sql.append(" ");
    }
}
```

**对比分析**：

| 功能 | MyBatis官方 | LightSSM |
|-----|-----------|----------|
| Token解析 | GenericTokenParser | 不支持 |
| 属性访问 | 完整的MetaObject | 基础MetaObject |
| 上下文Map | 支持嵌套属性 | 简化实现 |

## 7. XML解析器

### 7.1 XMLMapperBuilder

**LightSSM实现**：
```java
public class XMLMapperBuilder {
    private Configuration configuration;
    private XNode root;
    
    public void parse() {
        // 解析mapper根节点
        mapperElement(root.evalNode("mapper"));
    }
    
    private void mapperElement(XNode context) {
        // 解析namespace
        String namespace = context.getStringAttribute("namespace");
        
        // 解析select/insert/update/delete
        List<XNode> selectNodes = context.evalNodes("select");
        for (XNode node : selectNodes) {
            parseSelectNode(node, namespace);
        }
        
        // 解析resultMap
        List<XNode> resultMapNodes = context.evalNodes("resultMap");
        for (XNode node : resultMapNodes) {
            parseResultMapNode(node, namespace);
        }
        
        // 解析动态SQL标签
        parseDynamicTags(context);
    }
    
    private void parseSelectNode(XNode node, String namespace) {
        String id = node.getStringAttribute("id");
        String resultType = node.getStringAttribute("resultType");
        String statementId = namespace + "." + id;
        
        // 解析SQL内容
        SqlSource sqlSource = createSqlSource(node);
        
        // 创建MappedStatement
        MappedStatement.Builder statementBuilder = new MappedStatement.Builder(
            configuration, statementId, sqlSource, SqlCommandType.SELECT);
        statementBuilder.resultMaps(Collections.singletonList(
            new ResultMap.Builder(configuration, "defaultResultMap", 
                resultType, null).build()));
        
        configuration.addMappedStatement(statementBuilder.build());
    }
}
```

### 7.2 与MyBatis官方对比

**MyBatis官方的XMLMapperBuilder**：
- 支持完整的XML解析
- 支持include标签
- 支持cache和cache-ref
- 支持parameterMap
- 支持discriminator
- 支持完整的resultMap继承

**LightSSM简化**：
- 基础的select/insert/update/delete解析
- 基础的resultMap解析
- 不支持include/cache
- 不支持parameterMap

## 8. 设计模式总结

### 8.1 组合模式

```java
// SqlNode树形结构
public class MixedSqlNode implements SqlNode {
    private List<SqlNode> contents;
    
    @Override
    public boolean apply(DynamicContext context) {
        for (SqlNode sqlNode : contents) {
            sqlNode.apply(context);
        }
        return true;
    }
}
```

### 8.2 解释器模式

```java
// OGNL表达式解释器
public class ExpressionEvaluator {
    public Object evaluate(String expression, Object parameterObject) {
        // 解释并执行表达式
        return Ognl.getValue(expression, context, parameterObject);
    }
}
```

### 8.3 访问者模式

```java
// XNode访问XML节点
public class XNode {
    public String getStringAttribute(String name) { ... }
    public List<XNode> evalNodes(String expression) { ... }
    public XNode evalNode(String expression) { ... }
}
```

## 9. 面试考点

### 9.1 高频问题

1. **#{}和${}的区别？**
   - #{}：预编译，防止SQL注入
   - ${}：字符串替换，有SQL注入风险

2. **动态SQL有哪些标签？**
   - if/choose/when/otherwise
   - where/set/trim
   - foreach/bind

3. **MyBatis如何解析XML？**
   - XMLMapperBuilder
   - XPath解析
   - DOM4J库

### 9.2 深度问题

1. **动态SQL的执行流程？**
   - XML解析为SqlNode树
   - SqlNode.apply()生成SQL
   - OGNL表达式求值
   - 创建BoundSql

2. **OGNL表达式如何工作？**
   - 表达式编译缓存
   - 对象属性访问
   - 方法调用

## 10. 总结

LightSSM的SQL解析模块实现了：

- **静态SQL**：StaticSqlSource
- **动态SQL**：DynamicSqlSource + SqlNode树
- **OGNL表达式**：ExpressionEvaluator
- **XML解析**：XMLMapperBuilder
- **动态上下文**：DynamicContext

通过对照MyBatis官方源码，可以看到LightSSM保留了SQL解析的核心机制，剥离了高级特性（如缓存、include等），非常适合学习理解。

---

**上一篇**：[06 - 数据源与事务管理实现](06-数据源与事务管理实现.md)

**下一篇**：[08 - 类型系统与类型处理器](08-类型系统与类型处理器.md)
