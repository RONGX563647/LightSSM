package com.lightframework.ioc.event;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 优化版事件广播器
 * 使用类型索引加速事件分发，避免每次遍历所有监听器
 */
public class SimpleApplicationEventMulticaster implements ApplicationEventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(SimpleApplicationEventMulticaster.class);
    
    // 所有监听器列表（用于向后兼容和通用场景）
    private final java.util.List<ApplicationListener<?>> listeners = new CopyOnWriteArrayList<>();
    
    // 类型索引：事件类型 -> 监听器列表（加速事件分发）
    private final Map<Class<? extends ApplicationEvent>, List<ApplicationListener<?>>> typeIndexedListeners = 
            new ConcurrentHashMap<>(32);
    
    private Executor executor = null;
    
    // 缓存监听器的事件类型
    private final Map<Class<?>, Class<? extends ApplicationEvent>> listenerEventTypeCache = new ConcurrentHashMap<>(64);
    
    public void addListener(ApplicationListener<?> listener) {
        listeners.add(listener);
        // 添加到类型索引
        Class<? extends ApplicationEvent> eventType = getListenerEventType(listener);
        if (eventType != null) {
            typeIndexedListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
        }
    }
    
    public void removeListener(ApplicationListener<?> listener) {
        listeners.remove(listener);
        Class<? extends ApplicationEvent> eventType = getListenerEventType(listener);
        if (eventType != null) {
            List<ApplicationListener<?>> list = typeIndexedListeners.get(eventType);
            if (list != null) {
                list.remove(listener);
            }
        }
    }
    
    @Override
    public void publishEvent(ApplicationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }
        
        Class<? extends ApplicationEvent> eventType = event.getClass();
        
        // 使用类型索引查找匹配的监听器
        List<ApplicationListener<?>> matchedListeners = findMatchingListeners(eventType);
        
        for (ApplicationListener<?> listener : matchedListeners) {
            if (executor != null) {
                executor.execute(() -> {
                    try {
                        invokeListener(listener, event);
                    } catch (Exception e) {
                        logger.error("Async event listener failed: {}", listener.getClass().getName(), e);
                    }
                });
            } else {
                invokeListener(listener, event);
            }
        }
    }
    
    /**
     * 查找匹配事件类型的监听器（包括父类事件类型的监听器）
     */
    private List<ApplicationListener<?>> findMatchingListeners(Class<? extends ApplicationEvent> eventType) {
        List<ApplicationListener<?>> result = new ArrayList<>();
        
        // 添加精确匹配的监听器
        List<ApplicationListener<?>> exactMatches = typeIndexedListeners.get(eventType);
        if (exactMatches != null) {
            result.addAll(exactMatches);
        }
        
        // 添加父类事件类型的监听器
        Class<?> superclass = eventType.getSuperclass();
        while (superclass != null && ApplicationEvent.class.isAssignableFrom(superclass)) {
            List<ApplicationListener<?>> parentMatches = typeIndexedListeners.get(superclass);
            if (parentMatches != null) {
                result.addAll(parentMatches);
            }
            superclass = superclass.getSuperclass();
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    private void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
        try {
            ((ApplicationListener<ApplicationEvent>) listener).onApplicationEvent(event);
        } catch (Exception e) {
            // 事件监听器失败时记录错误但继续执行其他监听器，不中断整个事件发布流程
            logger.error("Error in event listener: {}. Event: {}", 
                listener.getClass().getName(), event.getClass().getSimpleName(), e);
        }
    }
    
    /**
     * 获取监听器的事件类型，优先从缓存读取
     */
    @SuppressWarnings("unchecked")
    private Class<? extends ApplicationEvent> getListenerEventType(Object listener) {
        Class<?> listenerClass = listener.getClass();
        return (Class<? extends ApplicationEvent>) listenerEventTypeCache.computeIfAbsent(listenerClass,
            clazz -> (Class<? extends ApplicationEvent>) resolveListenerEventType(listener));
    }
    
    /**
     * 解析监听器的事件类型
     */
    private Class<?> resolveListenerEventType(Object listener) {
        Class<?> clazz = listener.getClass();
        
        // 搜索直接接口
        Class<?> type = searchInterfaces(clazz.getGenericInterfaces());
        if (type != null) return type;
        
        // 递归搜索父类接口
        Class<?> superclass = clazz.getSuperclass();
        while (superclass != null && superclass != Object.class) {
            type = searchInterfaces(superclass.getGenericInterfaces());
            if (type != null) return type;
            superclass = superclass.getSuperclass();
        }
        
        return ApplicationEvent.class; // 默认监听所有事件
    }
    
    /**
     * 在泛型接口中搜索 ApplicationListener<T>
     */
    private Class<?> searchInterfaces(Type[] interfaces) {
        for (Type iface : interfaces) {
            if (iface instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) iface;
                if (pt.getRawType() == ApplicationListener.class) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                        return (Class<?>) typeArgs[0];
                    }
                }
            }
        }
        return null;
    }
    
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
