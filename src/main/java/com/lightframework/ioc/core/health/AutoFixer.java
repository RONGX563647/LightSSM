package com.lightframework.ioc.core.health;

import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * 自动修复构造器循环依赖。
 * 修复策略：在 BeanDefinition 中标记需要 @Lazy 的构造器参数位置，
 * 容器在 instantiateBean 时检测到标记后注入代理对象。
 */
public final class AutoFixer {

    private static final Logger logger = LoggerFactory.getLogger(AutoFixer.class);
    private static final String LAZY_PARAM_ATTR = "lazyConstructorParams";

    private AutoFixer() {}

    /**
     * 修复所有构造器循环依赖。
     * @return 修复数量
     */
    public static int fix(DefaultListableBeanFactory beanFactory,
                          List<CycleInfo> cycles) {
        int fixed = 0;
        for (CycleInfo cycle : cycles) {
            if (cycle.type() == CycleInfo.CycleType.CONSTRUCTOR_CYCLE) {
                fixed += fixConstructorCycle(beanFactory, cycle);
            }
        }
        return fixed;
    }

    private static int fixConstructorCycle(DefaultListableBeanFactory beanFactory,
                                            CycleInfo cycle) {
        // 1. 找到打断点的 Bean
        String breakBeanName = cycle.beanNames().get(cycle.breakPointIndex());
        BeanDefinition bd = beanFactory.getBeanDefinition(breakBeanName);
        if (bd == null) return 0;

        // 2. 找到构造器中需要 @Lazy 的参数位置
        int lazyParamIndex = findLazyParamIndex(beanFactory, cycle, breakBeanName);
        if (lazyParamIndex >= 0) {
            // 在 BeanDefinition 属性中标记需要 @Lazy 的构造器参数
            bd.setPropertyValue(LAZY_PARAM_ATTR, lazyParamIndex);
            logger.info("[Health Check] Auto-fixed constructor cycle at {}#param[{}]",
                        breakBeanName, lazyParamIndex);
            return 1;
        }
        return 0;
    }

    private static int findLazyParamIndex(DefaultListableBeanFactory beanFactory,
                                           CycleInfo cycle, String breakBeanName) {
        BeanDefinition bd = beanFactory.getBeanDefinition(breakBeanName);
        if (bd == null) return -1;
        Class<?> beanClass = bd.getBeanClass();
        if (beanClass == null) return -1;

        // 使用共享的构造器查找逻辑
        Constructor<?> targetCtor = DependencyGraphBuilder.findTargetConstructor(beanClass);
        if (targetCtor == null) return -1;

        Class<?>[] paramTypes = targetCtor.getParameterTypes();
        // 找到循环链中下一个 Bean 的名称
        int nextIdx = (cycle.breakPointIndex() + 1) % cycle.beanNames().size();
        String nextBeanName = cycle.beanNames().get(nextIdx);

        for (int i = 0; i < paramTypes.length; i++) {
            if (beanFactory.isTypeMatch(paramTypes[i], nextBeanName)) {
                return i;
            }
        }
        return -1;
    }
}
