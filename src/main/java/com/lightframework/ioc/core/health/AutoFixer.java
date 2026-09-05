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
    // TODO [L3][练习] 手写构造器循环自动修复 fix（在环的断点 Bean 上标记需要 @Lazy 的构造器参数下标，使容器注入代理打破构造期循环）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：A<->B 构造器循环，对其中一个标记 lazyConstructorParams 后，容器能成功实例化两者。
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
