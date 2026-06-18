package com.lightframework.spi.conversion;

public interface ConverterRegistry {
    <S, T> void addConverter(Converter<S, T> converter);

    <S, T> T convert(S source, Class<T> targetType);

    boolean canConvert(Class<?> sourceType, Class<?> targetType);
}
