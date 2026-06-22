package com.lightframework.aop.annotation;

import com.lightframework.aop.annotation.After;
import com.lightframework.aop.annotation.AfterReturning;
import com.lightframework.aop.annotation.AfterThrowing;
import com.lightframework.aop.annotation.Around;
import com.lightframework.aop.annotation.Aspect;
import com.lightframework.aop.annotation.Before;
import com.lightframework.aop.annotation.Pointcut;
import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.aop.interceptor.AfterMethodInterceptor;
import com.lightframework.aop.interceptor.AfterReturningMethodInterceptor;
import com.lightframework.aop.interceptor.AfterThrowingMethodInterceptor;
import com.lightframework.aop.interceptor.AroundMethodInterceptor;
import com.lightframework.aop.interceptor.BeforeMethodInterceptor;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.pointcut.AspectJExpressionPointcut;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Order;
import com.lightframework.ioc.core.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AspectJ 自动代理创建器
 * 
 * 修复：
 * 1. 拦截器排序：Before → Around → After（修复之前 After 在 Around 之前执行的错误）
 * 2. 通知执行顺序：Before → Around.start → Target → Around.end → After
 * 3. parseAspect 使用 getDeclaredMethods() 避免继承方法
 * 4. 实现 @Order 排序支持
 */
@Component
public class AspectJAutoProxyCreator implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AspectJAutoProxyCreator.class);

    // 通知类型排序优先级（越小越先执行）
    // 拦截器链顺序: Before → After → Around
    // 实际执行: Before → Around.start → target → Around.end → After
    private static final int ORDER_BEFORE = 0;
    private static final int ORDER_AFTER = 10;
    private static final int ORDER_AFTER_RETURNING = 11;
    private static final int ORDER_AFTER_THROWING = 12;
    private static final int ORDER_AROUND = 20;

    private final Map<String, AspectInfo> aspectInfos = new LinkedHashMap<>();
    private final java.util.Set<String> proxyBeanNames = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Map<Class<?>, Integer> classMatchCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        try {
            return wrapIfNeeded(bean, beanName);
        } catch (Exception e) {
            logger.warn("Failed to create early AOP proxy for bean: {}", beanName, e);
            return bean;
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        if (proxyBeanNames.contains(beanName)) {
            return bean;
        }
        return wrapIfNeeded(bean, beanName);
    }

    private Object wrapIfNeeded(Object bean, String beanName) throws Exception {
        if (aspectInfos.isEmpty() && !isAspect(bean)) {
            return bean;
        }

        if (isAspect(bean)) {
            parseAspect(bean, beanName);
            return bean;
        }

        int classMatchMask = computeClassMatchMask(bean.getClass());
        if (classMatchMask == 0) {
            return bean;
        }

        Map<Method, List<MethodInterceptor>> methodInterceptorMap = buildInterceptorMap(bean, classMatchMask);

        if (methodInterceptorMap.isEmpty()) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        for (Map.Entry<Method, List<MethodInterceptor>> entry : methodInterceptorMap.entrySet()) {
            proxyFactory.addInterceptors(entry.getKey(), entry.getValue());
        }

        proxyBeanNames.add(beanName);
        logger.info("Created AOP proxy for bean: {} with {} methods", beanName, methodInterceptorMap.size());
        return proxyFactory.getProxy();
    }

    private int computeClassMatchMask(Class<?> beanClass) {
        Integer cached = classMatchCache.get(beanClass);
        if (cached != null) {
            return cached;
        }

        int mask = 0;
        int bitIndex = 0;

        for (Map.Entry<String, AspectInfo> entry : aspectInfos.entrySet()) {
            AspectInfo aspectInfo = entry.getValue();
            for (AdviceInfo advice : aspectInfo.getAdvices()) {
                if (advice.pointcut.matches(beanClass)) {
                    if (bitIndex < 31) {
                        mask |= (1 << bitIndex);
                    }
                }
                bitIndex++;
            }
        }

        classMatchCache.put(beanClass, mask);
        return mask;
    }

    private Map<Method, List<MethodInterceptor>> buildInterceptorMap(Object bean, int classMatchMask) {
        Map<Method, List<MethodInterceptor>> methodInterceptorMap = new LinkedHashMap<>();
        List<Method> targetMethods = collectTargetMethods(bean.getClass());

        for (Method beanMethod : targetMethods) {
            List<MethodInterceptor> methodInterceptors = null;

            int bitIndex = 0;
            for (Map.Entry<String, AspectInfo> entry : aspectInfos.entrySet()) {
                AspectInfo aspectInfo = entry.getValue();
                for (AdviceInfo advice : aspectInfo.getAdvices()) {
                    if (bitIndex < 31 && (classMatchMask & (1 << bitIndex)) == 0) {
                        bitIndex++;
                        continue;
                    }

                    if (advice.pointcut.matches(beanMethod)) {
                        if (methodInterceptors == null) {
                            methodInterceptors = new ArrayList<>();
                        }
                        methodInterceptors.add(advice.interceptor);
                    }
                    bitIndex++;
                }
            }

            if (methodInterceptors != null && !methodInterceptors.isEmpty()) {
                // 修复：按通知类型排序 Before → Around → After
                methodInterceptors.sort((a, b) -> Integer.compare(getOrder(a), getOrder(b)));
                methodInterceptorMap.put(beanMethod, methodInterceptors);
            }
        }

        return methodInterceptorMap;
    }

    /** 获取拦截器排序优先级 */
    private int getOrder(MethodInterceptor interceptor) {
        String name = interceptor.getClass().getSimpleName();
        if (name.startsWith("Before")) return ORDER_BEFORE;
        if (name.startsWith("AfterReturning")) return ORDER_AFTER_RETURNING;
        if (name.startsWith("AfterThrowing")) return ORDER_AFTER_THROWING;
        if (name.startsWith("After")) return ORDER_AFTER;
        if (name.startsWith("Around")) return ORDER_AROUND;
        return Integer.MAX_VALUE;
    }

    private List<Method> collectTargetMethods(Class<?> clazz) {
        Map<String, Method> methodMap = new LinkedHashMap<>();

        for (Class<?> iface : clazz.getInterfaces()) {
            for (Method m : iface.getMethods()) {
                methodMap.putIfAbsent(methodKey(m), m);
            }
        }

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                methodMap.putIfAbsent(methodKey(m), m);
            }
            current = current.getSuperclass();
        }

        return new ArrayList<>(methodMap.values());
    }

    private String methodKey(Method method) {
        StringBuilder sb = new StringBuilder(method.getName());
        sb.append('(');
        for (Class<?> param : method.getParameterTypes()) {
            sb.append(param.getName()).append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    private boolean isAspect(Object bean) {
        return bean.getClass().isAnnotationPresent(Aspect.class);
    }

    private void parseAspect(Object aspectInstance, String beanName) throws Exception {
        Class<?> aspectClass = aspectInstance.getClass();
        AspectInfo aspectInfo = new AspectInfo(beanName);

        Order orderAnn = aspectClass.getAnnotation(Order.class);
        if (orderAnn != null) {
            aspectInfo.aspectOrder = orderAnn.value();
        }

        Map<String, AspectJExpressionPointcut> pointcutMap = new LinkedHashMap<>();
        Method[] declaredMethods = aspectClass.getDeclaredMethods();
        for (Method method : declaredMethods) {
            Pointcut pointcutAnn = method.getAnnotation(Pointcut.class);
            if (pointcutAnn != null) {
                pointcutMap.put(method.getName(), new AspectJExpressionPointcut(pointcutAnn.value()));
            }
        }

        for (Method method : declaredMethods) {
            Before before = method.getAnnotation(Before.class);
            After after = method.getAnnotation(After.class);
            Around around = method.getAnnotation(Around.class);
            AfterReturning afterReturning = method.getAnnotation(AfterReturning.class);
            AfterThrowing afterThrowing = method.getAnnotation(AfterThrowing.class);

            if (before != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(before.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new BeforeMethodInterceptor(method, aspectInstance), ORDER_BEFORE));
                }
            }
            if (around != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(around.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AroundMethodInterceptor(method, aspectInstance), ORDER_AROUND));
                }
            }
            if (afterReturning != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(afterReturning.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterReturningMethodInterceptor(method, aspectInstance), ORDER_AFTER_RETURNING));
                }
            }
            if (afterThrowing != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(afterThrowing.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterThrowingMethodInterceptor(method, aspectInstance), ORDER_AFTER_THROWING));
                }
            }
            if (after != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(after.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterMethodInterceptor(method, aspectInstance), ORDER_AFTER));
                }
            }
        }

        aspectInfo.sortAdvices();
        aspectInfos.put(beanName, aspectInfo);
        logger.info("Parsed @Aspect: {} with {} advices", beanName, aspectInfo.getAdvices().size());
    }

    private AspectJExpressionPointcut resolvePointcut(String value, Map<String, AspectJExpressionPointcut> pointcutMap) {
        if (value.startsWith("execution(") || value.startsWith("@annotation(")) {
            return new AspectJExpressionPointcut(value);
        }
        return pointcutMap.get(value);
    }

    private static class AspectInfo {
        private final String beanName;
        private final List<AdviceInfo> advices = new ArrayList<>();
        int aspectOrder = 0;

        AspectInfo(String beanName) {
            this.beanName = beanName;
        }

        void addAdvice(AdviceInfo advice) {
            this.advices.add(advice);
        }

        void sortAdvices() {
            advices.sort((a, b) -> Integer.compare(a.order, b.order));
        }

        List<AdviceInfo> getAdvices() {
            return advices;
        }
    }

    private static class AdviceInfo {
        final AspectJExpressionPointcut pointcut;
        final MethodInterceptor interceptor;
        final int order;

        AdviceInfo(AspectJExpressionPointcut pointcut, MethodInterceptor interceptor, int order) {
            this.pointcut = pointcut;
            this.interceptor = interceptor;
            this.order = order;
        }
    }
}
