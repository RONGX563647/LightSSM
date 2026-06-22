package com.lightframework.tx.interceptor;

import com.lightframework.aop.core.ProxyFactory;
import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Order;
import com.lightframework.ioc.core.BeanPostProcessor;
import com.lightframework.tx.annotation.Transactional;
import com.lightframework.tx.core.PlatformTransactionManager;
import com.lightframework.tx.core.TransactionAttributeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-1)
public class TransactionalBeanPostProcessor implements BeanPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalBeanPostProcessor.class);

    private final PlatformTransactionManager defaultTm;
    private final Map<String, PlatformTransactionManager> transactionManagers;
    private final TransactionAttributeSource attributeSource;
    private final java.util.Set<String> proxyBeanNames = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public TransactionalBeanPostProcessor(PlatformTransactionManager defaultTm) {
        this(defaultTm, Collections.emptyMap());
    }

    public TransactionalBeanPostProcessor(PlatformTransactionManager defaultTm,
                                           Map<String, PlatformTransactionManager> transactionManagers) {
        this.defaultTm = defaultTm;
        this.transactionManagers = transactionManagers;
        this.attributeSource = new TransactionAttributeSource();
    }

    @Override
    public Object getEarlyBeanReference(Object bean, String beanName) {
        try {
            return wrapIfNeeded(bean, beanName);
        } catch (Exception e) {
            logger.warn("Failed to create early TX proxy for bean: {}", beanName, e);
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
        Class<?> beanClass = bean.getClass();
        Map<Method, com.lightframework.aop.core.MethodInterceptor> txMethods = findTransactionalMethods(beanClass);
        if (txMethods.isEmpty()) {
            return bean;
        }
        // Pre-warm TransactionAttribute cache — moves reflection from first invoke to startup
        for (Method method : txMethods.keySet()) {
            attributeSource.getTransactionAttribute(method, beanClass);
        }
        ProxyFactory proxyFactory = createProxy(bean, txMethods);
        proxyBeanNames.add(beanName);
        if (logger.isDebugEnabled()) {
            logger.debug("Created @Transactional proxy for: {} ({} methods)", beanName, txMethods.size());
        }
        return proxyFactory.getProxy();
    }

    private Map<Method, com.lightframework.aop.core.MethodInterceptor> findTransactionalMethods(Class<?> beanClass) {
        Map<Method, com.lightframework.aop.core.MethodInterceptor> result = new LinkedHashMap<>();
        if (beanClass.isAnnotationPresent(Transactional.class)) {
            for (Method method : beanClass.getMethods()) {
                if (method.getDeclaringClass() != Object.class) {
                    result.put(method, createInterceptor());
                }
            }
            return result;
        }
        for (Method method : beanClass.getMethods()) {
            if (method.getDeclaringClass() != Object.class
                && method.isAnnotationPresent(Transactional.class)) {
                result.put(method, createInterceptor());
            }
        }
        return result;
    }

    private com.lightframework.aop.core.MethodInterceptor createInterceptor() {
        return new TransactionInterceptor(defaultTm, transactionManagers, attributeSource);
    }

    private ProxyFactory createProxy(Object bean,
                                      Map<Method, com.lightframework.aop.core.MethodInterceptor> txMethods) {
        ProxyFactory factory = new ProxyFactory(bean);
        for (Map.Entry<Method, com.lightframework.aop.core.MethodInterceptor> entry : txMethods.entrySet()) {
            factory.addInterceptor(entry.getKey(), entry.getValue());
        }
        return factory;
    }
}
