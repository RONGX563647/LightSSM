package com.lightframework.spi.conversion;

public interface Converter<S, T> {
    T convert(S source);

    Class<S> getSourceType();

    Class<T> getTargetType();
}
