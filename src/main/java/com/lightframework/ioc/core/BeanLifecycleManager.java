package com.lightframework.ioc.core;

import com.lightframework.ioc.beans.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean 生命周期管理器
 * 负责：@PostConstruct/@PreDestroy 执行、InitializingBean/DisposableBean 调用
 * 从 DefaultListableBeanFactory 中提取生命周期管理逻辑
 */
public class BeanLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(BeanLifecycleManager.class);

    // 缓存：类 → 方法签名 → Method，避免 override 方法重复执行
    private final Map<Class<?>, List<Method>> cachedPostConstructMethods = new ConcurrentHashMap<>(256);
    private final Map<Class<?>, List<Method>> cachedPreDestroyMethods = new ConcurrentHashMap<>(256);

    public void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) throws Exception {
        if (bean == null) return;
        Class<?> beanClass = bean.getClass();

        List<Method> postConstructMethods = cachedPostConstructMethods.computeIfAbsent(beanClass, clazz -> {
            List<Method> result = new ArrayList<>();
            collectAnnotatedMethods(clazz, PostConstruct.class, result);
            return result;
        });

        for (Method method : postConstructMethods) {
            try {
                method.setAccessible(true);
                method.invoke(bean);
            } catch (Exception e) {
                throw new Exception("@PostConstruct method '" + method.getName() + "' failed on bean '" + beanName + "'", e);
            }
            if (logger.isDebugEnabled()) logger.debug("Invoked @PostConstruct method: {} on bean {}", method.getName(), beanName);
        }

        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
            if (logger.isDebugEnabled()) logger.debug("Invoked afterPropertiesSet() on bean {}", beanName);
        }

        // ★ 支持自定义 initMethodName (如 @Bean(initMethod="init"))
        invokeCustomMethod(beanName, bean, bd != null ? bd.getInitMethodName() : null, "init");
    }

    public void invokeDestroyMethods(String beanName, Object bean) {
        invokeDestroyMethods(beanName, bean, null);
    }

    public void invokeDestroyMethods(String beanName, Object bean, BeanDefinition bd) {
        if (bean == null) return;
        Class<?> beanClass = bean.getClass();

        List<Method> preDestroyMethods = cachedPreDestroyMethods.computeIfAbsent(beanClass, clazz -> {
            List<Method> result = new ArrayList<>();
            collectAnnotatedMethods(clazz, PreDestroy.class, result);
            return result;
        });

        for (Method method : preDestroyMethods) {
            try {
                method.setAccessible(true);
                method.invoke(bean);
                if (logger.isDebugEnabled()) logger.debug("Invoked @PreDestroy method: {} on bean {}", method.getName(), beanName);
            } catch (Exception e) {
                logger.warn("Failed to invoke @PreDestroy on bean {}", beanName, e);
            }
        }

        if (bean instanceof DisposableBean) {
            try {
                ((DisposableBean) bean).destroy();
                if (logger.isDebugEnabled()) logger.debug("Invoked destroy() on bean {}", beanName);
            } catch (Exception e) {
                logger.warn("Failed to invoke destroy() on bean {}", beanName, e);
            }
        }

        // ★ 支持自定义 destroyMethodName
        invokeCustomMethod(beanName, bean, bd != null ? bd.getDestroyMethodName() : null, "destroy");
    }

    /**
     * 统一调用自定义方法（initMethod/destroyMethod）
     */
    private void invokeCustomMethod(String beanName, Object bean, String methodName, String phase) {
        if (methodName == null || methodName.isEmpty()) return;
        Class<?> beanClass = bean.getClass();
        try {
            Method method = beanClass.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(bean);
            if (logger.isDebugEnabled()) logger.debug("Invoked custom {} method '{}' on bean {}", phase, methodName, beanName);
        } catch (NoSuchMethodException e) {
            if (phase.equals("init")) {
                throw new IllegalStateException("Custom " + phase + " method '" + methodName + "' not found on bean '" + beanName + "'", e);
            }
            logger.warn("Custom {} method '{}' not found on bean '{}'", phase, methodName, beanName);
        } catch (Exception e) {
            logger.warn("Failed to invoke custom {} method '{}' on bean '{}'", phase, methodName, beanName, e);
        }
    }

    /**
     * 递归收集类层次结构中标注方法，通过方法签名去重避免 override 方法重复执行
     */
    private void collectAnnotatedMethods(Class<?> clazz, Class<? extends java.lang.annotation.Annotation> annotationClass,
                                          List<Method> result) {
        if (clazz == null || clazz == Object.class) return;
        collectAnnotatedMethods(clazz.getSuperclass(), annotationClass, result);

        Set<String> signatures = new HashSet<>();
        for (Method m : result) signatures.add(m.getName() + m.getParameterCount());

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isBridge()) continue;
            if (method.isAnnotationPresent(annotationClass)) {
                String sig = method.getName() + method.getParameterCount();
                // 子类 override 的方法覆盖父类同名方法
                if (!signatures.contains(sig)) {
                    result.add(method);
                    signatures.add(sig);
                }
            }
        }
    }
}
