package com.lightframework.aop.annotation;

import com.lightframework.aop.annotation.After;
import com.lightframework.aop.annotation.Around;
import com.lightframework.aop.annotation.Aspect;
import com.lightframework.aop.annotation.Before;
import com.lightframework.aop.annotation.Pointcut;
import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.aop.interceptor.AfterMethodInterceptor;
import com.lightframework.aop.interceptor.AroundMethodInterceptor;
import com.lightframework.aop.interceptor.BeforeMethodInterceptor;
import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.pointcut.AspectJExpressionPointcut;
import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.core.BeanPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AspectJAutoProxyCreator implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AspectJAutoProxyCreator.class);

    private final Map<String, AspectInfo> aspectInfos = new LinkedHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        if (isAspect(bean)) {
            parseAspect(bean, beanName);
            return bean;
        }

        if (aspectInfos.isEmpty()) {
            return bean;
        }

        Map<Method, List<MethodInterceptor>> methodInterceptorMap = new LinkedHashMap<>();
        for (Method beanMethod : bean.getClass().getMethods()) {
            List<MethodInterceptor> methodInterceptors = new ArrayList<>();
            for (AspectInfo aspectInfo : aspectInfos.values()) {
                for (AdviceInfo advice : aspectInfo.getAdvices()) {
                    if (advice.pointcut.matches(bean.getClass(), beanMethod)) {
                        methodInterceptors.add(advice.interceptor);
                    }
                }
            }
            if (!methodInterceptors.isEmpty()) {
                methodInterceptorMap.put(beanMethod, methodInterceptors);
            }
        }

        if (methodInterceptorMap.isEmpty()) {
            return bean;
        }

        ProxyFactory proxyFactory = new ProxyFactory(bean);
        for (Map.Entry<Method, List<MethodInterceptor>> entry : methodInterceptorMap.entrySet()) {
            proxyFactory.addInterceptors(entry.getKey(), entry.getValue());
        }

        logger.info("Created AOP proxy for bean: {} with {} method-mapped interceptors", beanName, methodInterceptorMap.size());
        return proxyFactory.getProxy();
    }

    private boolean isAspect(Object bean) {
        return bean.getClass().isAnnotationPresent(Aspect.class);
    }

    private void parseAspect(Object aspectInstance, String beanName) throws Exception {
        Class<?> aspectClass = aspectInstance.getClass();
        AspectInfo aspectInfo = new AspectInfo(beanName);

        Map<String, AspectJExpressionPointcut> pointcutMap = new LinkedHashMap<>();
        for (Method method : aspectClass.getMethods()) {
            Pointcut pointcutAnn = method.getAnnotation(Pointcut.class);
            if (pointcutAnn != null) {
                pointcutMap.put(method.getName(), new AspectJExpressionPointcut(pointcutAnn.value()));
            }
        }

        for (Method method : aspectClass.getMethods()) {
            Before before = method.getAnnotation(Before.class);
            After after = method.getAnnotation(After.class);
            Around around = method.getAnnotation(Around.class);

            if (before != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(before.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new BeforeMethodInterceptor(method, aspectInstance), method));
                }
            }
            if (after != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(after.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AfterMethodInterceptor(method, aspectInstance), method));
                }
            }
            if (around != null) {
                AspectJExpressionPointcut pointcut = resolvePointcut(around.value(), pointcutMap);
                if (pointcut != null) {
                    aspectInfo.addAdvice(new AdviceInfo(pointcut, new AroundMethodInterceptor(method, aspectInstance), method));
                }
            }
        }

        aspectInfos.put(beanName, aspectInfo);
        logger.info("Parsed @Aspect: {} with {} advices", beanName, aspectInfo.getAdvices().size());
    }

    private AspectJExpressionPointcut resolvePointcut(String value, Map<String, AspectJExpressionPointcut> pointcutMap) {
        if (value.startsWith("execution(")) {
            return new AspectJExpressionPointcut(value);
        }
        return pointcutMap.get(value);
    }

    private static class AspectInfo {
        private final String beanName;
        private final List<AdviceInfo> advices = new ArrayList<>();

        AspectInfo(String beanName) {
            this.beanName = beanName;
        }

        void addAdvice(AdviceInfo advice) {
            this.advices.add(advice);
        }

        List<AdviceInfo> getAdvices() {
            return advices;
        }
    }

    private static class AdviceInfo {
        final AspectJExpressionPointcut pointcut;
        final MethodInterceptor interceptor;
        final Method method;

        AdviceInfo(AspectJExpressionPointcut pointcut, MethodInterceptor interceptor, Method method) {
            this.pointcut = pointcut;
            this.interceptor = interceptor;
            this.method = method;
        }
    }
}
