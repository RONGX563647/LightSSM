package com.lightframework.mvc.handler;

import com.lightframework.mvc.annotation.PathVariable;
import com.lightframework.mvc.annotation.RequestParam;
import com.lightframework.mvc.annotation.ResponseBody;
import com.lightframework.mvc.core.HandlerAdapter;
import com.lightframework.mvc.core.ModelAndView;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class RequestMappingHandlerAdapter implements HandlerAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestMappingHandlerAdapter.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public boolean supports(Object handler) {
        return handler instanceof HandlerMethod;
    }
    
    @Override
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, 
        Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        Object[] args = resolveMethodArguments(handlerMethod, request, response);
        
        Object returnValue = invokeHandlerMethod(handlerMethod, args);
        
        return handleReturnValue(returnValue, handlerMethod, request, response);
    }
    
    protected Object[] resolveMethodArguments(HandlerMethod handlerMethod, 
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Method method = handlerMethod.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            Class<?> paramType = param.getType();
            
            if (HttpServletRequest.class.isAssignableFrom(paramType)) {
                args[i] = request;
            } else if (HttpServletResponse.class.isAssignableFrom(paramType)) {
                args[i] = response;
            } else {
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
    
    protected Object resolveRequestParam(RequestParam requestParam, Class<?> paramType, 
        HttpServletRequest request) {
        String paramName = requestParam.value();
        if (paramName.isEmpty()) {
            paramName = requestParam.name();
        }
        
        String value = request.getParameter(paramName);
        
        if (value == null) {
            if (requestParam.required()) {
                throw new IllegalArgumentException("Required parameter '" + paramName + "' is missing");
            }
            return null;
        }
        
        return convertValue(value, paramType);
    }
    
    protected Object resolvePathVariable(PathVariable pathVariable, Class<?> paramType, 
        HttpServletRequest request) {
        String varName = pathVariable.value();
        if (varName.isEmpty()) {
            varName = pathVariable.name();
        }
        
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath.length() > 0) {
            uri = uri.substring(contextPath.length());
        }
        
        String[] uriParts = uri.split("/");
        String value = uriParts[uriParts.length - 1];
        
        return convertValue(value, paramType);
    }
    
    protected Object resolveDefaultArgument(Class<?> paramType, HttpServletRequest request) {
        String paramName = paramType.getSimpleName();
        paramName = paramName.substring(0, 1).toLowerCase() + paramName.substring(1);
        
        String value = request.getParameter(paramName);
        if (value == null) {
            return null;
        }
        
        return convertValue(value, paramType);
    }
    
    protected Object convertValue(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        
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
    
    protected Object invokeHandlerMethod(HandlerMethod handlerMethod, Object[] args) 
        throws Exception {
        Object bean = handlerMethod.getBean();
        Method method = handlerMethod.getMethod();
        
        method.setAccessible(true);
        return method.invoke(bean, args);
    }
    
    protected ModelAndView handleReturnValue(Object returnValue, HandlerMethod handlerMethod, 
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        Method method = handlerMethod.getMethod();
        ResponseBody responseBody = method.getAnnotation(ResponseBody.class);
        
        if (responseBody != null) {
            handleResponseBody(returnValue, response);
            return null;
        }
        
        if (returnValue instanceof ModelAndView) {
            return (ModelAndView) returnValue;
        }
        
        if (returnValue instanceof String) {
            String viewName = (String) returnValue;
            return new ModelAndView(viewName);
        }
        
        if (returnValue instanceof Map) {
            Map<String, Object> model = (Map<String, Object>) returnValue;
            return new ModelAndView().addAllObjects(model);
        }
        
        return null;
    }
    
    protected void handleResponseBody(Object returnValue, HttpServletResponse response) 
        throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        
        String json = objectMapper.writeValueAsString(returnValue);
        
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
        
        logger.debug("Written JSON response: {}", json);
    }
    
    @Override
    public long getLastModified(HttpServletRequest request, Object handler) {
        return -1;
    }
}