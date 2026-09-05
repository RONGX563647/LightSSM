package com.lightframework.ioc.core;

public interface BeanFactory {
    // TODO [L1][练习] 手写最简 BeanFactory 实现（用 Map<String,Object> 持有单例，getBean(name) 直接返回，getBean(Class) 按类型查找）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：register("a", obj) 后 getBean("a")==obj；getBean(A.class) 返回类型匹配的那个。
    Object getBean(String name) throws Exception;
    
    <T> T getBean(String name, Class<T> requiredType) throws Exception;
    
    <T> T getBean(Class<T> requiredType) throws Exception;
    
    boolean containsBean(String name);
    
    boolean isSingleton(String name);
    
    boolean isPrototype(String name);
    
    Class<?> getType(String name);
    
    String[] getAliases(String name);
}