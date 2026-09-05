package com.lightframework.ioc.core;

import com.lightframework.ioc.beans.BeanDefinition;

/**
 * BeanDefinition 注册表接口
 * 兼容 Spring 的 BeanDefinitionRegistry
 */
public interface BeanDefinitionRegistry {
    // TODO [L1][练习] 手写 BeanDefinitionRegistry（用 Map 存 BeanDefinition + 别名映射，registerAlias 需处理链式解析与循环别名检测）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：registerBeanDefinition("a", bd) 后 containsBeanDefinition("a")==true；registerAlias("b","a") 后 getBeanDefinition("b")==bd。
    void registerBeanDefinition(String beanName, BeanDefinition beanDefinition);
    void removeBeanDefinition(String beanName) throws Exception;
    BeanDefinition getBeanDefinition(String beanName);
    boolean containsBeanDefinition(String beanName);
    String[] getBeanDefinitionNames();
    int getBeanDefinitionCount();
    void registerAlias(String beanName, String alias);
}
