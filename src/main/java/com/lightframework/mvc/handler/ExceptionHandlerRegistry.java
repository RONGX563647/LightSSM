package com.lightframework.mvc.handler;

import com.lightframework.mvc.annotation.ExceptionHandler;
import com.lightframework.mvc.core.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExceptionHandlerRegistry {
    private final Map<Class<? extends Throwable>, HandlerMethod> mappedHandlers = new ConcurrentHashMap<>();

    public void registerHandler(Class<? extends Throwable> exceptionType, HandlerMethod handlerMethod) {
        mappedHandlers.put(exceptionType, handlerMethod);
    }

    public HandlerMethod findHandler(Throwable ex) {
        Class<?> exceptionClass = ex.getClass();
        HandlerMethod handler = mappedHandlers.get(exceptionClass);
        if (handler != null) return handler;

        for (Map.Entry<Class<? extends Throwable>, HandlerMethod> entry : mappedHandlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void registerFromController(Object controller, Class<?> controllerClass) {
        for (Method method : controllerClass.getDeclaredMethods()) {
            ExceptionHandler ann = method.getAnnotation(ExceptionHandler.class);
            if (ann != null) {
                Class<? extends Throwable>[] exceptionTypes = ann.value();
                if (exceptionTypes.length == 0) {
                    exceptionTypes = new Class[]{Throwable.class};
                }
                for (Class<? extends Throwable> exType : exceptionTypes) {
                    HandlerMethod hm = new HandlerMethod(controller, method);
                    registerHandler(exType, hm);
                }
            }
        }
    }

    public boolean isEmpty() {
        return mappedHandlers.isEmpty();
    }
}
