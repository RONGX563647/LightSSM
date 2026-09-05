package com.lightframework.mvc.handler;

import com.lightframework.mvc.annotation.ExceptionHandler;
import com.lightframework.mvc.core.ModelAndView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExceptionHandlerRegistry {
    // TODO [L3][优化-观察者模式] 异常处理可改为观察者模式：异常发生时向已注册的 @ExceptionHandler 订阅者广播，；写对标志：按观察者模式完成实现，新增单测覆盖“主题状态变更后所有观察者被通知”的主路径与一条注销后不再收到通知的路径。
    //   由订阅者按"异常类型最匹配"原则竞争处理，新增异常类型无需改动分发逻辑即可被订阅。
    private final Map<Class<? extends Throwable>, HandlerMethod> mappedHandlers = new ConcurrentHashMap<>();

    public void registerHandler(Class<? extends Throwable> exceptionType, HandlerMethod handlerMethod) {
        mappedHandlers.put(exceptionType, handlerMethod);
    }

    public HandlerMethod findHandler(Throwable ex) {
        Class<?> exceptionClass = ex.getClass();
        HandlerMethod handler = mappedHandlers.get(exceptionClass);
        if (handler != null) return handler;

        // TODO [L1][练习] 当前按 Map 遍历顺序做 isAssignableFrom 匹配，命中第一个父类，无法保证"最具体的异常类型优先"；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   请收集所有 isAssignableFrom 命中的类型，选出距离 exceptionClass 最近的那个（子类优先）。验收标准：同时注册 Exception 与 RuntimeException 时，RuntimeException 先命中。
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
