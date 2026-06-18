package com.lightframework.ioc.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 默认类型转换器实现
 * 支持基本类型、包装类型、枚举类型的转换
 */
public class DefaultTypeConverter implements TypeConverter {

    // 类型转换表（不可变，线程安全）
    private final Map<Class<?>, Function<String, Object>> converterTable;

    // 自定义转换器注册表（线程安全）
    private final Map<Class<?>, TypeConverter> customConverters = new ConcurrentHashMap<>();

    public DefaultTypeConverter() {
        this.converterTable = Collections.unmodifiableMap(buildConverterTable());
    }

    @Override
    public boolean supports(Class<?> targetType) {
        return converterTable.containsKey(targetType)
            || targetType.isEnum()
            || customConverters.containsKey(targetType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T convert(Object source, Class<T> targetType) throws Exception {
        if (source == null) {
            return null;
        }

        // 如果已经是目标类型，直接返回
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }

        // 检查自定义转换器
        TypeConverter custom = customConverters.get(targetType);
        if (custom != null) {
            return custom.convert(source, targetType);
        }

        // String 转目标类型
        if (source instanceof String) {
            return convertFromString((String) source, targetType);
        }

        // 不支持的转换，尝试直接 cast
        return targetType.cast(source);
    }

    @SuppressWarnings("unchecked")
    private <T> T convertFromString(String value, Class<T> targetType) {
        Function<String, Object> converter = converterTable.get(targetType);
        if (converter != null) {
            return (T) converter.apply(value);
        }

        // 支持枚举类型转换
        if (targetType.isEnum()) {
            try {
                return (T) Enum.valueOf((Class<Enum>) targetType, value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    "Invalid enum value '" + value + "' for type " + targetType.getName() +
                    ". Valid values: " + Arrays.toString(targetType.getEnumConstants()));
            }
        }

        // 不支持的转换，抛出异常而不是隐藏错误
        throw new IllegalArgumentException("Cannot convert String '" + value + "' to type " + targetType.getName());
    }

    /**
     * 注册自定义类型转换器
     */
    public void registerConverter(Class<?> targetType, TypeConverter converter) {
        if (targetType == null || converter == null) {
            throw new IllegalArgumentException("targetType and converter must not be null");
        }
        customConverters.put(targetType, converter);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * 构建默认类型转换表
     */
    private static Map<Class<?>, Function<String, Object>> buildConverterTable() {
        Map<Class<?>, Function<String, Object>> table = new HashMap<>(32);
        table.put(String.class, v -> v);
        table.put(Integer.class, Integer::valueOf);
        table.put(Integer.TYPE, Integer::valueOf);
        table.put(Long.class, Long::valueOf);
        table.put(Long.TYPE, Long::valueOf);
        table.put(Boolean.class, Boolean::valueOf);
        table.put(Boolean.TYPE, Boolean::valueOf);
        table.put(Double.class, Double::valueOf);
        table.put(Double.TYPE, Double::valueOf);
        table.put(Float.class, Float::valueOf);
        table.put(Float.TYPE, Float::valueOf);
        table.put(Short.class, Short::valueOf);
        table.put(Short.TYPE, Short::valueOf);
        table.put(Byte.class, Byte::valueOf);
        table.put(Byte.TYPE, Byte::valueOf);
        table.put(Character.class, v -> v.isEmpty() ? null : v.charAt(0));
        table.put(Character.TYPE, v -> v.isEmpty() ? '\0' : v.charAt(0));
        return table;
    }
}
