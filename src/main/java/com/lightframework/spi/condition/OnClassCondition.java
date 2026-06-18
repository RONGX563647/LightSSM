package com.lightframework.spi.condition;

import com.lightframework.ioc.core.Condition;
import com.lightframework.ioc.core.ConditionContext;
import com.lightframework.spi.annotation.ConditionalOnClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class OnClassCondition implements Condition {
    private static final Logger logger = LoggerFactory.getLogger(OnClassCondition.class);

    public static final ThreadLocal<String> currentClassName = new ThreadLocal<>();

    private static final ConcurrentHashMap<String, Boolean> classExistsCache = new ConcurrentHashMap<>(64);

    @Override
    public boolean matches(ConditionContext context) {
        String className = currentClassName.get();
        if (className == null) {
            return true;
        }
        try {
            Class<?> clazz = Class.forName(className, false, context.getClassLoader());
            ConditionalOnClass annotation = clazz.getAnnotation(ConditionalOnClass.class);
            if (annotation == null) {
                return true;
            }
            for (String requiredClass : annotation.value()) {
                if (checkClassExists(requiredClass, context)) {
                    logger.debug("@ConditionalOnClass({}) satisfied for {}", requiredClass, className);
                    return true;
                } else {
                    logger.debug("@ConditionalOnClass({}) not satisfied for {}", requiredClass, className);
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    private static boolean checkClassExists(String className, ConditionContext context) {
        Boolean cached = classExistsCache.get(className);
        if (cached != null) {
            return cached;
        }
        try {
            Class.forName(className, false, context.getClassLoader());
            classExistsCache.put(className, Boolean.TRUE);
            return true;
        } catch (ClassNotFoundException e) {
            classExistsCache.put(className, Boolean.FALSE);
            return false;
        }
    }

    static void clearCache() {
        classExistsCache.clear();
    }
}
