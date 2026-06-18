package com.lightframework.ioc.core;

/**
 * 类型转换器 SPI 接口
 * 设计模式：策略模式 - 允许自定义类型转换逻辑
 */
public interface TypeConverter {
    /**
     * 是否支持转换到目标类型
     */
    boolean supports(Class<?> targetType);

    /**
     * 执行类型转换
     */
    <T> T convert(Object source, Class<T> targetType) throws Exception;

    /**
     * 优先级（数值越小优先级越高）
     */
    default int getOrder() {
        return 100;
    }
}
