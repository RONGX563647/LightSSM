package com.lightframework.mvc.convert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConverterRegistry {
    private final List<Converter<String, ?>> converters = new CopyOnWriteArrayList<>();

    public void addConverter(Converter<String, ?> converter) {
        converters.add(converter);
    }

    public void removeConverter(Converter<String, ?> converter) {
        converters.remove(converter);
    }

    @SuppressWarnings("unchecked")
    public <T> Converter<String, T> findConverter(Class<T> targetType) {
        for (Converter<String, ?> converter : converters) {
            if (converter.getTargetType().equals(targetType)) {
                return (Converter<String, T>) converter;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> T convert(String value, Class<T> targetType) {
        if (value == null) return null;
        Converter<String, T> converter = findConverter(targetType);
        if (converter != null) {
            return converter.convert(value);
        }
        return null;
    }

    public List<Converter<String, ?>> getConverters() {
        return converters;
    }
}
