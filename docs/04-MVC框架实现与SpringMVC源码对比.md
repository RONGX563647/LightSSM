# 04 - MVC框架实现与Spring MVC源码对比

## 1. Spring MVC概述

Spring MVC是基于Servlet API构建的原始Web框架，从Spring框架一开始就包含在其中。

### 1.1 核心架构

```
客户端请求
    ↓
DispatcherServlet（前端控制器）
    ↓
HandlerMapping（处理器映射）
    ↓
HandlerAdapter（处理器适配器）
    ↓
Controller（处理器）
    ↓
ModelAndView（模型和视图）
    ↓
ViewResolver（视图解析器）
    ↓
View（视图渲染）
    ↓
响应客户端
```

### 1.2 核心组件

| 组件 | 职责 |
|-----|------|
| DispatcherServlet | 前端控制器，统一分发请求 |
| HandlerMapping | 根据URL映射到处理器 |
| HandlerAdapter | 适配不同类型的处理器 |
| Controller | 处理请求，返回结果 |
| ModelAndView | 封装模型数据和视图名称 |
| ViewResolver | 根据视图名称解析视图对象 |
| View | 渲染视图，生成响应 |

## 2. DispatcherServlet前端控制器

### 2.1 核心职责

DispatcherServlet是Spring MVC的核心，负责：
- 接收HTTP请求
- 查找处理器
- 执行处理器
- 渲染视图
- 返回响应

### 2.2 初始化流程

**LightSSM实现**（[DispatcherServlet.java:L37-92](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/servlet/DispatcherServlet.java#L37-L92)）：

```java
@Override
public void init() throws ServletException {
    try {
        // 1. 初始化应用上下文（IoC容器）
        initApplicationContext();
        
        // 2. 初始化处理器映射
        initHandlerMappings();
        
        // 3. 初始化处理器适配器
        initHandlerAdapters();
        
        // 4. 初始化视图解析器
        initViewResolvers();
        
        logger.info("DispatcherServlet initialized successfully");
    } catch (Exception e) {
        logger.error("Failed to initialize DispatcherServlet", e);
        throw new ServletException("Failed to initialize DispatcherServlet", e);
    }
}
```

### 2.3 与Spring官方对比

**Spring官方的init流程**（FrameworkServlet）：

```java
// Spring官方初始化流程
public class DispatcherServlet extends FrameworkServlet {
    
    @Override
    protected void onRefresh(ApplicationContext context) {
        initStrategies(context);
    }
    
    protected void initStrategies(ApplicationContext context) {
        initMultipartResolver(context);        // 文件上传解析器
        initLocaleResolver(context);           // 区域解析器
        initThemeResolver(context);            // 主题解析器
        initHandlerMappings(context);          // 处理器映射
        initHandlerAdapters(context);          // 处理器适配器
        initHandlerExceptionResolvers(context); // 异常处理器
        initRequestToViewNameTranslator(context); // 请求到视图名转换器
        initViewResolvers(context);            // 视图解析器
        initFlashMapManager(context);          // 闪存映射管理器
    }
}
```

**对比分析**：

| 初始化项 | Spring官方 | LightSSM |
|---------|-----------|----------|
| IoC容器 | FrameworkServlet管理 | 自己创建ApplicationContext |
| 处理器映射 | 从容器获取或创建默认 | 创建RequestMappingHandlerMapping |
| 处理器适配器 | 从容器获取或创建默认 | 创建RequestMappingHandlerAdapter |
| 视图解析器 | 从容器获取或创建默认 | 从容器获取或创建InternalResourceViewResolver |
| 文件上传 | MultipartResolver | 不支持 |
| 区域解析 | LocaleResolver | 不支持 |
| 异常处理 | HandlerExceptionResolver | 简单handleError方法 |
| 主题解析 | ThemeResolver | 不支持 |

## 3. 请求处理流程

### 3.1 doDispatch核心方法

**LightSSM实现**（[DispatcherServlet.java:L105-136](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/servlet/DispatcherServlet.java#L105-L136)）：

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    
    try {
        // 1. 获取处理器执行链
        mappedHandler = getHandler(processedRequest);
        if (mappedHandler == null) {
            noHandlerFound(processedRequest, response);
            return;
        }
        
        // 2. 获取处理器适配器
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        if (ha == null) {
            throw new ServletException("No adapter for handler [" + mappedHandler.getHandler() + "]");
        }
        
        // 3. 执行拦截器前置处理
        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return;
        }
        
        // 4. 执行处理器
        ModelAndView mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        
        // 5. 执行拦截器后置处理
        mappedHandler.applyPostHandle(processedRequest, response, mv);
        
        // 6. 处理结果（渲染视图）
        processDispatchResult(processedRequest, response, mappedHandler, mv);
        
    } catch (Exception ex) {
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        throw ex;
    }
}
```

### 3.2 与Spring官方对比

**Spring官方的doDispatch**（简化版）：

```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean errorView = false;
    
    try {
        ModelAndView mv = null;
        Exception dispatchException = null;
        
        try {
            // 1. 处理文件上传请求
            processedRequest = checkMultipart(request);
            
            // 2. 获取HandlerExecutionChain
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }
            
            // 3. 获取HandlerAdapter
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
            
            // 4. 处理last-modified
            String method = request.getMethod();
            if ("GET".equals(method) || "HEAD".equals(method)) {
                long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                if (new ServletWebRequest(request, response).checkNotModified(lastModified)) {
                    return;
                }
            }
            
            // 5. 执行拦截器preHandle
            if (!mappedHandler.applyPreHandle(processedRequest, response, mappedHandler.getHandler())) {
                return;
            }
            
            // 6. 执行处理器
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
            
            if (asyncManager.isConcurrentHandlingStarted()) {
                return; // 异步处理
            }
            
            // 7. 应用默认视图名称
            applyDefaultViewName(processedRequest, mv);
            
            // 8. 执行拦截器postHandle
            mappedHandler.applyPostHandle(processedRequest, response, mv);
        } catch (Exception ex) {
            dispatchException = ex;
        }
        
        // 9. 处理结果
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
    } catch (Exception ex) {
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    } finally {
        // 10. 清理
        if (asyncManager.isConcurrentHandlingStarted()) {
            // 异步清理
        } else {
            resetContextAfterRequest();
        }
    }
}
```

**对比分析**：

| 步骤 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 文件上传 | checkMultipart处理 | 不支持 |
| Handler获取 | 相同 | 相同 |
| Adapter获取 | 相同 | 相同 |
| last-modified | 支持HTTP缓存 | 不支持 |
| 拦截器preHandle | 相同 | 相同 |
| 处理器执行 | 相同 | 相同 |
| 异步处理 | 支持 | 不支持 |
| 默认视图名 | applyDefaultViewName | 不支持 |
| 拦截器postHandle | 相同 | 相同 |
| 结果处理 | 包含异常处理 | 简化版 |
| 异步清理 | 支持 | 不支持 |

## 4. HandlerMapping处理器映射

### 4.1 HandlerMapping接口

**LightSSM定义**：
```java
public interface HandlerMapping {
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```

**Spring官方定义**：
```java
public interface HandlerMapping {
    String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";
    String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";
    // ... 多个常量
    
    HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
}
```

### 4.2 RequestMappingHandlerMapping

**LightSSM实现**（[RequestMappingHandlerMapping.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/handler/RequestMappingHandlerMapping.java)）：

```java
public class RequestMappingHandlerMapping implements HandlerMapping {
    
    // 路径 -> HandlerMethod映射
    private final Map<String, HandlerMethod> handlerMethods = new ConcurrentHashMap<>(256);
    
    // 路径 -> HandlerExecutionChain缓存
    private final Map<String, HandlerExecutionChain> handlerCache = new ConcurrentHashMap<>(256);
    
    private final ApplicationContext applicationContext;
    
    public void initHandlerMethods() throws Exception {
        // 扫描所有Bean
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            Class<?> beanType = applicationContext.getType(beanName);
            // 检查是否有@RequestMapping注解
            if (beanType != null && isHandler(beanType)) {
                detectHandlerMethods(beanName);
            }
        }
    }
    
    protected boolean isHandler(Class<?> beanType) {
        return beanType.getAnnotation(RequestMapping.class) != null;
    }
    
    protected void detectHandlerMethods(String beanName) throws Exception {
        Class<?> handlerType = applicationContext.getType(beanName);
        Method[] methods = handlerType.getDeclaredMethods();
        
        for (Method method : methods) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                registerHandlerMethod(beanName, method, mapping);
            }
        }
    }
    
    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        String lookupPath = getLookupPath(request);
        
        // 先查缓存
        HandlerExecutionChain handlerChain = this.handlerCache.get(lookupPath);
        if (handlerChain != null) {
            return handlerChain;
        }
        
        // 查找HandlerMethod
        HandlerMethod handlerMethod = this.handlerMethods.get(lookupPath);
        if (handlerMethod == null) {
            return null;
        }
        
        // 构建执行链（包含拦截器）
        handlerChain = getHandlerExecutionChain(handlerMethod, request);
        this.handlerCache.put(lookupPath, handlerChain);
        
        return handlerChain;
    }
}
```

### 4.3 与Spring官方对比

**Spring官方的RequestMappingHandlerMapping**核心逻辑：

```java
public class RequestMappingHandlerMapping extends AbstractHandlerMethodMapping<RequestMappingInfo> {
    
    @Override
    protected boolean isHandler(Class<?> beanType) {
        return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
                AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
    }
    
    @Override
    protected void detectHandlerMethods(Object handler) {
        Class<?> handlerType = (handler instanceof String) ?
                obtainApplicationContext().getType((String) handler) : handler.getClass();
        
        if (handlerType != null) {
            Class<?> userType = ClassUtils.getUserClass(handlerType);
            // 查找所有匹配的方法
            Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
                (MethodIntrospector.MetadataLookup<T>) method -> {
                    try {
                        return getMappingForMethod(method, userType);
                    } catch (Throwable ex) {
                        throw new IllegalStateException("...");
                    }
                });
            methods.forEach((method, mapping) -> {
                Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
                registerHandlerMethod(handler, invocableMethod, mapping);
            });
        }
    }
    
    protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
        RequestMappingInfo info = createRequestMappingInfo(method);
        if (info != null) {
            RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
            if (typeInfo != null) {
                info = typeInfo.combine(info); // 合并类和方法级别的映射
            }
        }
        return info;
    }
}
```

**对比分析**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 处理器识别 | @Controller或@RequestMapping | 仅@RequestMapping |
| 映射信息 | RequestMappingInfo（复杂对象） | 简单路径字符串 |
| 类级别映射 | 支持合并 | 不支持 |
| HTTP方法映射 | 支持GET/POST/PUT/DELETE | 不支持 |
| 参数条件映射 | params/headers条件 | 不支持 |
| 缓存机制 | 复杂的MappingRegistry | 简单ConcurrentHashMap |
| 路径匹配 | PathPatternMatcher | 精确匹配 |
| 通配符 | 支持*和** | 不支持 |

## 5. HandlerAdapter处理器适配器

### 5.1 适配器模式的作用

HandlerAdapter的作用是统一不同类型处理器的调用方式。

```
不同类型处理器：
- HandlerMethod（注解处理器）
- HttpRequestHandler（HTTP请求处理器）
- SimpleControllerHandlerAdapter（传统Controller）

通过HandlerAdapter统一为：
ModelAndView handle(request, response, handler)
```

### 5.2 RequestMappingHandlerAdapter

**LightSSM实现**（[RequestMappingHandlerAdapter.java](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/handler/RequestMappingHandlerAdapter.java)）：

```java
public class RequestMappingHandlerAdapter implements HandlerAdapter {
    
    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }
    
    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, 
        Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 1. 解析方法参数
        Object[] args = resolveMethodArguments(handlerMethod, request, response);
        
        // 2. 调用处理器方法
        Object returnValue = invokeHandlerMethod(handlerMethod, args);
        
        // 3. 处理返回值
        return handleReturnValue(returnValue, handlerMethod, request, response);
    }
}
```

### 5.3 参数解析

**LightSSM参数解析**（[RequestMappingHandlerAdapter.java:L42-121](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/handler/RequestMappingHandlerAdapter.java#L42-L121)）：

```java
protected Object[] resolveMethodArguments(HandlerMethod handlerMethod, 
    HttpServletRequest request, HttpServletResponse response) throws Exception {
    Method method = handlerMethod.getMethod();
    Parameter[] parameters = method.getParameters();
    Object[] args = new Object[parameters.length];
    
    for (int i = 0; i < parameters.length; i++) {
        Parameter param = parameters[i];
        Class<?> paramType = param.getType();
        
        // 内置类型参数
        if (HttpServletRequest.class.isAssignableFrom(paramType)) {
            args[i] = request;
        } else if (HttpServletResponse.class.isAssignableFrom(paramType)) {
            args[i] = response;
        } else {
            // 注解参数
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);
            
            if (requestParam != null) {
                args[i] = resolveRequestParam(requestParam, paramType, request);
            } else if (pathVariable != null) {
                args[i] = resolvePathVariable(pathVariable, paramType, request);
            } else {
                args[i] = resolveDefaultArgument(paramType, request);
            }
        }
    }
    
    return args;
}
```

### 5.4 与Spring官方对比

**Spring官方的HandlerMethodArgumentResolver体系**：

```java
public interface HandlerMethodArgumentResolver {
    boolean supportsParameter(MethodParameter parameter);
    Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception;
}

// 多个实现：
// - RequestParamMethodArgumentResolver
// - PathVariableMethodArgumentResolver  
// - RequestBodyMethodArgumentResolver
// - ModelMethodArgumentResolver
// - SessionAttributeMethodArgumentResolver
// - ... 20+种解析器
```

**对比分析**：

| 参数类型 | Spring官方 | LightSSM |
|---------|-----------|----------|
| HttpServletRequest | 支持 | 支持 |
| HttpServletResponse | 支持 | 支持 |
| @RequestParam | RequestParamMethodArgumentResolver | resolveRequestParam |
| @PathVariable | PathVariableMethodArgumentResolver | resolvePathVariable |
| @RequestBody | RequestResponseBodyMethodProcessor | 不支持 |
| @ModelAttribute | ModelAttributeMethodProcessor | 不支持 |
| HttpSession | SessionAttributeMethodArgumentResolver | 不支持 |
| Principal | PrincipalMethodArgumentResolver | 不支持 |
| 类型转换 | ConversionService | convertValue（基础类型） |

## 6. 返回值处理

### 6.1 LightSSM的返回值处理

**源码位置**：[RequestMappingHandlerAdapter.java:L152-191](file:///e:/CODE/developed-project/LightSSM/light-framework/src/main/java/com/lightframework/mvc/handler/RequestMappingHandlerAdapter.java#L152-L191)

```java
protected ModelAndView handleReturnValue(Object returnValue, HandlerMethod handlerMethod, 
    HttpServletRequest request, HttpServletResponse response) throws Exception {
    
    Method method = handlerMethod.getMethod();
    ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
    
    // @ResponseBody：直接写入JSON响应
    if (responseBody != null) {
        handleResponseBody(returnValue, response);
        return null; // 不需要视图渲染
    }
    
    // ModelAndView：直接返回
    if (returnValue instanceof ModelAndView) {
        return (ModelAndView) returnValue;
    }
    
    // String：作为视图名称
    if (returnValue instanceof String) {
        String viewName = (String) returnValue;
        return new ModelAndView(viewName);
    }
    
    // Map：作为模型数据
    if (returnValue instanceof Map) {
        Map<String, Object> model = (Map<String, Object>) returnValue;
        return new ModelAndView().addAllObjects(model);
    }
    
    return null;
}

protected void handleResponseBody(Object returnValue, HttpServletResponse response) 
    throws Exception {
    response.setContentType("application/json;charset=UTF-8");
    
    // 使用Jackson序列化JSON
    String json = objectMapper.writeValueAsString(returnValue);
    
    PrintWriter writer = response.getWriter();
    writer.write(json);
    writer.flush();
}
```

### 6.2 与Spring官方对比

**Spring官方的HandlerMethodReturnValueHandler体系**：

```java
public interface HandlerMethodReturnValueHandler {
    boolean supportsReturnType(MethodParameter returnType);
    void handleReturnValue(Object returnValue, MethodParameter returnType,
            ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception;
}

// 多个实现：
// - RequestResponseBodyMethodProcessor（@ResponseBody）
// - ViewNameMethodReturnValueHandler（String视图名）
// - ModelAndViewMethodReturnValueHandler（ModelAndView）
// - ModelMethodProcessor（Model）
// - HttpEntityMethodProcessor（HttpEntity）
// - ... 15+种处理器
```

**对比分析**：

| 返回值类型 | Spring官方 | LightSSM |
|-----------|-----------|----------|
| @ResponseBody | RequestResponseBodyMethodProcessor | handleResponseBody |
| ModelAndView | ModelAndViewMethodReturnValueHandler | 直接返回 |
| String（视图名） | ViewNameMethodReturnValueHandler | new ModelAndView |
| Map | ModelMethodProcessor | addAllObjects |
| void | 默认处理 | 返回null |
| ResponseEntity | HttpEntityMethodProcessor | 不支持 |
| StreamingResponseBody | StreamingResponseBodyReturnValueHandler | 不支持 |

## 7. HandlerExecutionChain执行链

### 7.1 设计目的

HandlerExecutionChain封装了处理器和拦截器，形成完整的执行链。

### 7.2 LightSSM实现

```java
public class HandlerExecutionChain {
    private Object handler;                            // 处理器
    private List<HandlerInterceptor> interceptorList;  // 拦截器列表
    
    // 拦截器preHandle（任一返回false则中断）
    public boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) 
        throws Exception {
        if (this.interceptorList != null) {
            for (int i = 0; i < this.interceptorList.size(); i++) {
                HandlerInterceptor interceptor = this.interceptorList.get(i);
                if (!interceptor.preHandle(request, response, this.handler)) {
                    triggerAfterCompletion(request, response, null);
                    return false;
                }
            }
        }
        return true;
    }
    
    // 拦截器postHandle（逆序执行）
    public void applyPostHandle(HttpServletRequest request, HttpServletResponse response, 
        ModelAndView mv) throws Exception {
        if (this.interceptorList != null) {
            for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
                HandlerInterceptor interceptor = this.interceptorList.get(i);
                interceptor.postHandle(request, response, this.handler, mv);
            }
        }
    }
    
    // 拦截器afterCompletion（逆序执行，总会执行）
    public void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, 
        Exception ex) throws Exception {
        if (this.interceptorList != null) {
            for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
                HandlerInterceptor interceptor = this.interceptorList.get(i);
                interceptor.afterCompletion(request, response, this.handler, ex);
            }
        }
    }
}
```

### 7.3 拦截器执行顺序

```
请求进入
    ↓
Interceptor1.preHandle()
    ↓
Interceptor2.preHandle()
    ↓
Handler处理
    ↓
Interceptor2.postHandle()
    ↓
Interceptor1.postHandle()
    ↓
渲染视图
    ↓
Interceptor2.afterCompletion()
    ↓
Interceptor1.afterCompletion()
    ↓
响应返回
```

### 7.4 与Spring官方对比

Spring官方的HandlerExecutionChain与LightSSM实现几乎一致，主要差异：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 拦截器接口 | HandlerInterceptor | HandlerInterceptor |
| preHandle | 相同 | 相同 |
| postHandle | 相同 | 相同 |
| afterCompletion | 相同 | 相同 |
| 异步拦截器 | AsyncHandlerInterceptor | 不支持 |
| mappedInterceptors | 支持路径匹配 | 不支持 |

## 8. ViewResolver视图解析器

### 8.1 ViewResolver接口

```java
public interface ViewResolver {
    View resolveViewName(String viewName) throws Exception;
}
```

### 8.2 InternalResourceViewResolver

**LightSSM默认视图解析器**：

```java
public class InternalResourceViewResolver implements ViewResolver {
    private String prefix = "/WEB-INF/views/";
    private String suffix = ".jsp";
    
    @Override
    public View resolveViewName(String viewName) throws Exception {
        String viewPath = prefix + viewName + suffix;
        return new InternalResourceView(viewPath);
    }
}
```

### 8.3 JsonView（REST API支持）

```java
public class JsonView implements View {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, 
        HttpServletResponse response) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        String json = objectMapper.writeValueAsString(model);
        response.getWriter().write(json);
    }
}
```

### 8.4 与Spring官方对比

**Spring官方的ViewResolver层次**：

```
ViewResolver（接口）
├── AbstractCachingViewResolver（缓存基类）
│   ├── UrlBasedViewResolver（URL基类）
│   │   ├── InternalResourceViewResolver
│   │   ├── FreeMarkerViewResolver
│   │   └── ... 
│   └── ResourceBundleViewResolver
├── XmlViewResolver
├── BeanNameViewResolver
└── ContentNegotiatingViewResolver
```

**对比分析**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 缓存 | AbstractCachingViewResolver | 无缓存 |
| 前缀后缀 | 支持配置 | 硬编码 |
| 视图类型 | JSP/Freemarker/Thymeleaf等 | JSP/JSON/Redirect |
| 内容协商 | ContentNegotiatingViewResolver | 不支持 |
| 顺序 | Ordered接口 | 不支持 |

## 9. 参数绑定与类型转换

### 9.1 LightSSM的类型转换

```java
protected Object convertValue(String value, Class<?> targetType) {
    if (value == null) return null;
    
    if (String.class.equals(targetType)) {
        return value;
    } else if (Integer.class.equals(targetType) || int.class.equals(targetType)) {
        return Integer.parseInt(value);
    } else if (Long.class.equals(targetType) || long.class.equals(targetType)) {
        return Long.parseLong(value);
    } else if (Double.class.equals(targetType) || double.class.equals(targetType)) {
        return Double.parseDouble(value);
    } else if (Boolean.class.equals(targetType) || boolean.class.equals(targetType)) {
        return Boolean.parseBoolean(value);
    }
    
    return value;
}
```

### 9.2 与Spring官方对比

**Spring官方的ConversionService**：

```java
public interface ConversionService {
    boolean canConvert(Class<?> sourceType, Class<?> targetType);
    <T> T convert(Object source, Class<T> targetType);
}

// 内置转换器：
// - String -> Integer/Long/Double/Float/Boolean
// - String -> Date/Time/LocalDateTime
// - String -> Enum
// - String -> URL/URI/UUID
// - String -> Class
// - String -> Locale/Charset/Currency
// - String -> byte[]
// - ... 100+种转换
```

**对比分析**：

| 功能 | Spring官方 | LightSSM |
|-----|-----------|----------|
| 基础类型转换 | 支持 | 支持 |
| 日期转换 | DateTimeFormatter | 不支持 |
| 枚举转换 | 支持 | 不支持 |
| 自定义转换器 | Converter接口 | 不支持 |
| 格式化 | @DateTimeFormat/@NumberFormat | 不支持 |
| 数据绑定 | DataBinder | 不支持 |

## 10. 设计模式总结

### 10.1 前端控制器模式

```java
// DispatcherServlet是前端控制器
// 统一入口，分发请求到不同处理器
public class DispatcherServlet extends HttpServlet {
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        HandlerExecutionChain mappedHandler = getHandler(request);
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        ModelAndView mv = ha.handle(request, response, mappedHandler.getHandler());
        processDispatchResult(request, response, mappedHandler, mv);
    }
}
```

### 10.2 适配器模式

```java
// 适配器接口
public interface HandlerAdapter {
    boolean supports(Object handler);
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler);
}

// 具体适配器
public class RequestMappingHandlerAdapter implements HandlerAdapter { ... }
```

### 10.3 策略模式

```java
// HandlerMapping策略
HandlerMapping strategy1 = new RequestMappingHandlerMapping(context);

// HandlerAdapter策略
HandlerAdapter strategy2 = new RequestMappingHandlerAdapter();

// ViewResolver策略
ViewResolver strategy3 = new InternalResourceViewResolver();
```

### 10.4 责任链模式

```java
// HandlerInterceptor责任链
public class HandlerExecutionChain {
    public boolean applyPreHandle(...) {
        for (HandlerInterceptor interceptor : interceptorList) {
            if (!interceptor.preHandle(...)) {
                return false; // 中断链
            }
        }
        return true;
    }
}
```

## 11. 面试考点

### 11.1 高频问题

1. **Spring MVC的执行流程是什么？**
   - DispatcherServlet接收请求
   - HandlerMapping查找处理器
   - HandlerAdapter执行处理器
   - ViewResolver解析视图
   - View渲染响应

2. **DispatcherServlet的作用是什么？**
   - 前端控制器
   - 统一分发请求
   - 协调各组件工作

3. **HandlerMapping和HandlerAdapter的区别？**
   - HandlerMapping负责找到处理器
   - HandlerAdapter负责执行处理器

4. **@RequestParam和@PathVariable的区别？**
   - @RequestParam：查询参数
   - @PathVariable：路径变量

### 11.2 深度问题

1. **为什么要使用HandlerAdapter？**
   - 统一不同类型处理器的调用方式
   - 支持扩展新的处理器类型

2. **拦截器的执行顺序是什么？**
   - preHandle：正序执行
   - postHandle：逆序执行
   - afterCompletion：逆序执行

3. **Spring MVC是如何支持RESTful API的？**
   - @ResponseBody注解
   - HttpMessageConverter转换
   - Jackson序列化JSON

## 12. 总结

LightSSM的MVC模块完整实现了Spring MVC的核心机制：

- **前端控制器**：DispatcherServlet统一分发
- **处理器映射**：RequestMappingHandlerMapping注解映射
- **处理器适配**：RequestMappingHandlerAdapter参数解析
- **视图解析**：InternalResourceViewResolver视图渲染
- **拦截器链**：HandlerExecutionChain横切逻辑

通过对照Spring官方源码，可以看到LightSSM保留了MVC的核心流程，剥离了高级特性（如文件上传、异步处理、内容协商等），非常适合学习理解MVC原理。

---

**上一篇**：[03 - AOP代理机制与Spring AOP源码对比](03-AOP代理机制与SpringAOP源码对比.md)

**下一篇**：[05 - ORM核心架构与MyBatis源码对比](05-ORM核心架构与MyBatis源码对比.md)
