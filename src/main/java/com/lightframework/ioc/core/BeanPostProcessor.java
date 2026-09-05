package com.lightframework.ioc.core;

public interface BeanPostProcessor {
    // TODO [L1][练习] 手写一个 BeanPostProcessor（如给所有 Bean 统一设置某字段，或用代理包装），理解 BPP 在初始化前后对 Bean 的拦截。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：实现 postProcessAfterInitialization 处理 bean，getBean 拿到的是被处理后的实例。
    default Object postProcessBeforeInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
    
    default Object postProcessAfterInitialization(Object bean, String beanName) throws Exception {
        return bean;
    }
    
    default Object getEarlyBeanReference(Object bean, String beanName) {
        return bean;
    }
}