package com.lightframework.spi.condition;

import com.lightframework.ioc.core.Condition;
import com.lightframework.ioc.core.ConditionContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnMissingBeanCondition implements Condition {
    private static final Logger logger = LoggerFactory.getLogger(OnMissingBeanCondition.class);

    public static final ThreadLocal<String> currentClassName = new ThreadLocal<>();

    @Override
    public boolean matches(ConditionContext context) {
        String className = currentClassName.get();
        if (className == null) {
            return true;
        }
        try {
            Class<?> clazz = Class.forName(className, false, context.getClassLoader());
            ConditionalOnMissingBean annotation = clazz.getAnnotation(ConditionalOnMissingBean.class);
            if (annotation == null) {
                return true;
            }
            Class<?>[] beanTypes = annotation.value();
            if (beanTypes.length == 0) {
                return true;
            }
            if (!(context.getRegistry() instanceof DefaultListableBeanFactory)) {
                return true;
            }
            DefaultListableBeanFactory bf = (DefaultListableBeanFactory) context.getRegistry();
            for (Class<?> beanType : beanTypes) {
                String[] names = bf.getBeanNamesForType(beanType);
                if (names != null && names.length > 0) {
                    logger.debug("@ConditionalOnMissingBean({}) - bean exists, skipping {}", beanType.getSimpleName(), className);
                    return false;
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
