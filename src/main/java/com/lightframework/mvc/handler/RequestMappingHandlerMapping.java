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