package com.lightframework.mvc.convert;

public interface Converter<S, T> {
    T convert(S source);

    Class<S> getSourceType();

    Class<T> getTargetType();
}
