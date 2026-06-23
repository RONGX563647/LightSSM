package com.lightframework.tx.core;

import com.lightframework.tx.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionAttributeSource {

    private static final Transactional NO_ANNOTATION = new Transactional() {
        @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Transactional.class; }
        @Override public String value() { return ""; }
        @Override public com.lightframework.tx.annotation.Propagation propagation() { return com.lightframework.tx.annotation.Propagation.REQUIRED; }
        @Override public com.lightframework.tx.annotation.Isolation isolation() { return com.lightframework.tx.annotation.Isolation.DEFAULT; }
        @Override public int timeout() { return -1; }
        @Override public boolean readOnly() { return false; }
        @Override public Class<? extends Throwable>[] rollbackFor() { return new Class[0]; }
        @Override public Class<? extends Throwable>[] noRollbackFor() { return new Class[0]; }
        @Override public Class<? extends Throwable>[] retryFor() { return new Class[0]; }
        @Override public int maxRetries() { return 0; }
        @Override public int retryDelayMs() { return 0; }
    };

    private final ConcurrentHashMap<Method, TransactionAttribute> methodCache = new ConcurrentHashMap<>(64);
    private final ConcurrentHashMap<Class<?>, Transactional> classAnnCache = new ConcurrentHashMap<>(32);

    public TransactionAttribute getTransactionAttribute(Method method, Class<?> targetClass) {
        Method specificMethod = findSpecificMethod(method, targetClass);
        TransactionAttribute attr = methodCache.get(specificMethod);
        if (attr != null) {
            return attr;
        }
        Transactional txAnn = specificMethod.getAnnotation(Transactional.class);
        if (txAnn == null) {
            Class<?> clazz = targetClass;
            Transactional classTx = null;
            while (clazz != null && clazz != Object.class) {
                Transactional cached = classAnnCache.get(clazz);
                if (cached == NO_ANNOTATION) break;
                if (cached != null) {
                    classTx = cached;
                    break;
                }
                classTx = clazz.getAnnotation(Transactional.class);
                if (classTx != null) {
                    classAnnCache.put(clazz, classTx);
                    break;
                }
                classAnnCache.put(clazz, NO_ANNOTATION);
                clazz = clazz.getSuperclass();
            }
            if (classTx != null) {
                attr = new TransactionAttribute(classTx);
                methodCache.put(specificMethod, attr);
            }
            return attr;
        }
        attr = new TransactionAttribute(txAnn);
        methodCache.put(specificMethod, attr);
        return attr;
    }

    private Method findSpecificMethod(Method method, Class<?> targetClass) {
        if (method.getDeclaringClass() == targetClass) {
            return method;
        }
        try {
            return targetClass.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method;
        }
    }
}
