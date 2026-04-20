# 第3阶段：SpringMVC核心实现

## 变动内容

### 1. MVC注解定义（annotation包）
- `@RequestMapping` - 请求映射注解，支持path、method属性
- `@ResponseBody` - 响应体注解，返回JSON数据
- `@RequestParam` - 请求参数绑定注解
- `@PathVariable` - 路径变量绑定注解

### 2. 核心接口（core包）
- `HandlerMapping` - 处理器映射接口
  - getHandler()获取处理器执行链
- `HandlerAdapter` - 处理器适配接口
  - handle()执行处理器方法
- `HandlerExecutionChain` - 处理器执行链
  - 包含Handler和拦截器链
  - applyPreHandle()、applyPostHandle()、triggerAfterCompletion()
- `HandlerInterceptor` - 处理器拦截器接口
  - preHandle()、postHandle()、afterCompletion()
- `ModelAndView` - 模型视图对象
  - viewName、model属性
  - addObject()添加模型数据

### 3. 处理器实现（handler包）
- `HandlerMethod` - 处理器方法封装
  - beanName、method、beanType属性
- `RequestMappingHandlerMapping` - RequestMapping处理器映射
  - initHandlerMethods()初始化处理器方法
  - detectHandlerMethods()检测处理器方法
  - getHandler()获取处理器执行链
- `RequestMappingHandlerAdapter` - RequestMapping处理器适配
  - resolveMethodArguments()解析方法参数
  - invokeHandlerMethod()执行处理器方法
  - handleReturnValue()处理返回值
  - handleResponseBody()处理JSON响应

### 4. 核心分发器（servlet包）
- `DispatcherServlet` - 核心请求分发器
  - initHandlerMappings()初始化处理器映射
  - initHandlerAdapters()初始化处理器适配
  - initViewResolvers()初始化视图解析器
  - doDispatch()核心分发逻辑
  - processDispatchResult()处理分发结果

### 5. 视图解析（view包）
- `View` - 视图接口
  - render()渲染视图
- `ViewResolver` - 视图解析器接口
  - resolveViewName()解析视图名称
- `InternalResourceView` - 内部资源视图（JSP）
- `InternalResourceViewResolver` - 内部资源视图解析器
  - prefix、suffix配置
  - 支持redirect:、forward:前缀
- `RedirectView` - 重定向视图
- `JsonView` - JSON视图

## 设计说明

### SpringMVC核心流程

```
HTTP请求
    ↓
DispatcherServlet (核心分发器)
    ↓
HandlerMapping (处理器映射)
    ↓ 找到HandlerMethod
HandlerExecutionChain (执行链)
    ↓ 包含拦截器链
    ↓ applyPreHandle()前置拦截
HandlerAdapter (处理器适配)
    ↓ 执行Handler方法
    ↓ resolveMethodArguments()参数解析
    ↓ invokeHandlerMethod()方法执行
    ↓ handleReturnValue()返回值处理
ModelAndView (模型视图)
    ↓
ViewResolver (视图解析)
    ↓ resolveViewName()
View.render() (视图渲染)
    ↓
HTTP响应
```

### RESTful接口支持

**@ResponseBody注解**：
```java
@RequestMapping("/api/users")
@ResponseBody
public List<User> getUsers() {
    return userService.findAll();
}
```

**处理流程**：
1. RequestMappingHandlerAdapter检测到@ResponseBody注解
2. 调用handleResponseBody()方法
3. 使用Jackson ObjectMapper序列化为JSON
4. 设置响应头Content-Type: application/json;charset=UTF-8
5. 输出JSON字符串到response

### 参数绑定机制

**@RequestParam参数绑定**：
```java
@RequestMapping("/user")
public User getUser(@RequestParam("id") Long id) {
    return userService.getById(id);
}
```

**@PathVariable路径变量**：
```java
@RequestMapping("/user/{id}")
public User getUser(@PathVariable("id") Long id) {
    return userService.getById(id);
}
```

**参数解析流程**：
1. resolveMethodArguments()遍历方法参数
2. 检测@RequestParam/@PathVariable注解
3. 从request获取参数值
4. convertValue()类型转换（String→Long/Integer等）
5. 注入到方法参数

## 文件清单
- src/main/java/com/lightframework/mvc/annotation/*.java（4个注解）
- src/main/java/com/lightframework/mvc/core/*.java（5个核心类）
- src/main/java/com/lightframework/mvc/handler/*.java（3个处理器类）
- src/main/java/com/lightframework/mvc/servlet/DispatcherServlet.java
- src/main/java/com/lightframework/mvc/view/*.java（6个视图类）
- docs/stage3-SpringMVC实现.md

## 下一步计划
第4阶段将完善ORM模块，包括：
- 复制现有ORM代码到新项目
- 保持原有功能不变