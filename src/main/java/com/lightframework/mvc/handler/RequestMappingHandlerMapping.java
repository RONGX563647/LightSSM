package com.lightframework.mvc.handler;

import com.lightframework.di.annotation.Controller;
import com.lightframework.mvc.annotation.RequestMapping;
import com.lightframework.mvc.core.HandlerExecutionChain;
import com.lightframework.mvc.core.HandlerMapping;
import com.lightframework.ioc.context.ApplicationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestMappingHandlerMapping implements HandlerMapping {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestMappingHandlerMapping.class);
    
    private final Map<RequestMappingInfo, HandlerMethod> handlerMethods = new ConcurrentHashMap<>(256);
    
    private final Map<String, HandlerExecutionChain> handlerCache = new ConcurrentHashMap<>(256);
    
    private final ApplicationContext applicationContext;
    
    private List<com.lightframework.mvc.core.HandlerInterceptor> globalInterceptors = List.of();
    
    public RequestMappingHandlerMapping(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    public void initHandlerMethods() throws Exception {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType != null && isHandler(beanType)) {
                detectHandlerMethods(beanName);
            }
        }
        
        initGlobalInterceptors();
        warmupHandlerCache();
        
        logger.info("Mapped {} handler methods, {} global interceptors, {} routes warmed",
            this.handlerMethods.size(), this.globalInterceptors.size(), this.handlerCache.size());
    }
    
    protected void initGlobalInterceptors() {
        String[] interceptorNames = applicationContext.getBeanNamesForType(
            com.lightframework.mvc.core.HandlerInterceptor.class);
        List<com.lightframework.mvc.core.HandlerInterceptor> interceptors = new ArrayList<>(interceptorNames.length);
        for (String name : interceptorNames) {
            try {
                interceptors.add(applicationContext.getBean(name,
                    com.lightframework.mvc.core.HandlerInterceptor.class));
            } catch (Exception e) {
                logger.warn("Could not load interceptor: {}", name);
            }
        }
        this.globalInterceptors = List.copyOf(interceptors);
    }
    
    protected void warmupHandlerCache() {
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : this.handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            HandlerMethod handlerMethod = entry.getValue();
            String path = info.getPattern();
            String httpMethod = info.getMethod();
            if (!httpMethod.isEmpty()) {
                String cacheKey = httpMethod + ":" + path;
                this.handlerCache.putIfAbsent(cacheKey, buildExecutionChain(handlerMethod));
            }
            String cacheKeyAny = "GET:" + path;
            this.handlerCache.putIfAbsent(cacheKeyAny, buildExecutionChain(handlerMethod));
        }
    }
    
    protected HandlerExecutionChain buildExecutionChain(HandlerMethod handlerMethod) {
        // TODO [L3][优化-责任链] 当前所有 globalInterceptors 无差别地挂到每个 chain；可引入"拦截器匹配规则"（按 path pattern / HTTP 方法选择），；写对标志：按责任链模式完成实现，新增单测覆盖“链上节点处理/传递/终止”的主路径与一条全链放行的路径。
        //   用责任链/选择器在装配 chain 时只加入命中的拦截器，使不同路径组拥有不同的拦截器集合。
        HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);
        for (com.lightframework.mvc.core.HandlerInterceptor interceptor : globalInterceptors) {
            chain.addInterceptor(interceptor);
        }
        return chain;
    }
    
    protected boolean isHandler(Class<?> beanType) {
        return beanType.getAnnotation(Controller.class) != null
            || beanType.getAnnotation(RequestMapping.class) != null;
    }
    
    protected void detectHandlerMethods(String beanName) throws Exception {
        Class<?> handlerType = applicationContext.getType(beanName);
        if (handlerType == null) {
            return;
        }
        
        String classPrefix = resolveClassPrefix(handlerType);
        
        Method[] methods = handlerType.getDeclaredMethods();
        for (Method method : methods) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            String methodPath = null;
            String httpMethod = "";
            if (mapping != null) {
                methodPath = getPath(mapping);
                httpMethod = mapping.method();
            } else {
                for (java.lang.annotation.Annotation ann : method.getAnnotations()) {
                    RequestMapping metaMapping = ann.annotationType().getAnnotation(RequestMapping.class);
                    if (metaMapping != null) {
                        methodPath = resolveComposedPath(ann);
                        httpMethod = metaMapping.method();
                        // TODO [L2][练习] 当前组合注解（@GetMapping 等）只取了 path 与 method；请补充解析 RequestMapping 的；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
                        //   params / headers / consumes / produces 条件，使映射能按"请求参数/请求头/Content-Type/Accept"进一步匹配。
                        //   验收标准：@PostMapping(consumes="application/json") 只接收 JSON 请求，其他 Content-Type 不匹配。
                        break;
                    }
                }
            }
            if (methodPath != null) {
                String fullPath = classPrefix + methodPath;
                registerHandlerMethod(beanName, method, fullPath, httpMethod);
            }
        }
    }
    
    protected String resolveComposedPath(java.lang.annotation.Annotation composedAnnotation) {
        try {
            java.lang.reflect.Method valueMethod = composedAnnotation.annotationType().getMethod("value");
            String path = (String) valueMethod.invoke(composedAnnotation);
            if (path.isEmpty()) {
                java.lang.reflect.Method pathMethod = composedAnnotation.annotationType().getMethod("path");
                String[] paths = (String[]) pathMethod.invoke(composedAnnotation);
                if (paths.length > 0) {
                    path = paths[0];
                }
            }
            return path;
        } catch (Exception e) {
            return "";
        }
    }
    
    protected String resolveClassPrefix(Class<?> handlerType) {
        // TODO [L1][练习] 类级 @RequestMapping 前缀拼接（补前导 "/"，去尾部 "/"）目前逻辑分散；请抽取为 normalizePath(path) 工具方法，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   统一处理空串、重复 "//"、以及多级拼接（/api + /v1）的规范化。验收标准：输入 "api/" 与 "/api" 都规范成 "/api"。
        RequestMapping classMapping = handlerType.getAnnotation(RequestMapping.class);
        if (classMapping != null) {
            String prefix = getPath(classMapping);
            if (!prefix.isEmpty() && !prefix.startsWith("/")) {
                prefix = "/" + prefix;
            }
            if (prefix.endsWith("/")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }
            return prefix;
        }
        return "";
    }
    
    protected void registerHandlerMethod(String beanName, Method method, String path, String httpMethod) 
        throws Exception {
        HandlerMethod handlerMethod = new HandlerMethod(beanName, method, applicationContext);
        
        // TODO [L3][优化-工厂方法] 当前 RequestMappingInfo 的创建散落在多处（registerHandlerMethod、warmupHandlerCache、getHandler）；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
        //   可用工厂方法 createMappingInfo(annotation, path, httpMethod) 统一构造并缓存，便于集中补充匹配条件（params/headers/consumes）。
        this.handlerMethods.put(new RequestMappingInfo(path, httpMethod), handlerMethod);
        logger.debug("Mapped \"{}\" [{}] to {}", path, httpMethod, handlerMethod);
    }
    
    protected String getPath(RequestMapping mapping) {
        String path = mapping.value();
        if (path.isEmpty() && mapping.path().length > 0) {
            path = mapping.path()[0];
        }
        return path;
    }
    
    @Override
    public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        String lookupPath = getLookupPath(request);
        String httpMethod = request.getMethod();

        // TODO [L2][练习] 当前遍历 handlerMethods 用第一个 match 胜出，没有"匹配优先级"；请实现优先级：；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   精确路径 > 带路径变量的模板（/user/{id}）> 含正则/通配的模板，使更具体的路由优先命中。
        //   验收标准：同时注册 /user/profile 与 /user/{id} 时，请求 /user/profile 命中前者。
        String cacheKey = httpMethod + ":" + lookupPath;
        HandlerExecutionChain handlerChain = this.handlerCache.get(cacheKey);
        if (handlerChain != null) {
            return handlerChain;
        }
        
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : this.handlerMethods.entrySet()) {
            RequestMappingInfo info = entry.getKey();
            Map<String, String> variables = info.match(lookupPath, httpMethod);
            if (variables != null) {
                request.setAttribute(RequestMappingInfo.PATH_VARIABLES_ATTRIBUTE, variables);
                HandlerMethod handlerMethod = entry.getValue();
                handlerChain = buildExecutionChain(handlerMethod);
                this.handlerCache.put(cacheKey, handlerChain);
                return handlerChain;
            }
        }
        
        return null;
    }
    
    protected String getLookupPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath.length() > 0) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }
    
    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }
    
    public Map<String, HandlerMethod> getHandlerMethods() {
        Map<String, HandlerMethod> result = new LinkedHashMap<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : this.handlerMethods.entrySet()) {
            result.put(entry.getKey().getPattern(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }
}