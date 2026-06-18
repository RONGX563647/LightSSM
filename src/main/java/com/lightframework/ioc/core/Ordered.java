package com.lightframework.ioc.core;

/**
 * 接口用于定义 BeanPostProcessor 的执行顺序
 * 值越小，优先级越高
 */
public interface Ordered {
    /**
     * 获取顺序值
     * @return 顺序值，越小优先级越高
     */
    int getOrder();
}
