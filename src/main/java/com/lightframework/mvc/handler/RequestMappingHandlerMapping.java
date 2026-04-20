package com.lightframework.mvc.handler;

import com.lightframework.mvc.annotation.RequestMapping;
import com.lightframework.mvc.core.HandlerExecutionChain;
import com.lightframework.mvc.core.HandlerMapping;
import com.lightframework.ioc.context.ApplicationContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RequestMappingHandlerMapping implements HandlerMapping {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestMappingHandlerMapping.class);
    
    private final Map<String, HandlerMethod> handlerMethods = new ConcurrentHashMap<>(256);
    
    private final Map<String, HandlerExecutionChain> handlerCache = new ConcurrentHashMap<>(256);
    
    private final ApplicationContext applicationContext;
    
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
        
        logger.info("Mapped {} handler methods", this.handlerMethods.size());
    }
    
    protected boolean isHandler(Class<?> beanType) {
        return beanType.getAnnotation(RequestMapping.class) != null;
    }
    
    protected void detectHandlerMethods(String beanName) throws Exception {
        Class<?> handlerType = applicationContext.getType(beanName);
        if (handlerType == null) {
            return;
        }
        
        Method[] methods = handlerType.getDeclaredMethods();
        for (Method method : methods) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                registerHandlerMethod(beanName, method, mapping);
            }
        }
    }
    
    protected void registerHandlerMethod(String beanName, Method method, RequestMapping mapping) 
        throws Exception {
        String path = getPath(mapping);
        HandlerMethod handlerMethod = new HandlerMethod(beanName, method, applicationContext);
        
        this.handlerMethods.put(path, handlerMethod);
        logger.debug("Mapped \"{}\" to {}", path, handlerMethod);
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
        
        HandlerExecutionChain handlerChain = this.handlerCache.get(lookupPath);
        if (handlerChain != null) {
            return handlerChain;
        }
        
        HandlerMethod handlerMethod = this.handlerMethods.get(lookupPath);
        if (handlerMethod == null) {
            return null;
        }
        
        handlerChain = getHandlerExecutionChain(handlerMethod, request);
        this.handlerCache.put(lookupPath, handlerChain);
        
        return handlerChain;
    }
    
    protected String getLookupPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath.length() > 0) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }
    
    protected HandlerExecutionChain getHandlerExecutionChain(HandlerMethod handlerMethod, 
        HttpServletRequest request) {
        HandlerExecutionChain chain = new HandlerExecutionChain(handlerMethod);
        
        String[] interceptorNames = applicationContext.getBeanNamesForType(
            com.lightframework.mvc.core.HandlerInterceptor.class);
        for (String name : interceptorNames) {
            try {
                chain.addInterceptor(applicationContext.getBean(name, 
                    com.lightframework.mvc.core.HandlerInterceptor.class));
            } catch (Exception e) {
                logger.warn("Could not load interceptor: {}", name);
            }
        }
        
        return chain;
    }
    
    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }
    
    public Map<String, HandlerMethod> getHandlerMethods() {
        return Collections.unmodifiableMap(this.handlerMethods);
    }
}