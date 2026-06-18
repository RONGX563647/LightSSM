package com.lightframework.di.core;

public interface TypeConverter {
    boolean supports(Class<?> targetType);
    <T> T convert(Object source, Class<T> targetType) throws Exception;
    default int getOrder() {
        return 100;
    }
}
