package com.lightframework.di.core;

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

        // TODO [L1][练习] 手写「源对象已是目标类型则直接短路返回」的判断——targetType.isInstance(source) 为真时直接 targetType.cast(source) 返回，避免无谓的字符串转换；写对标志：传入 Integer 且目标为 Number/Object 时返回同一个对象引用。
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

    // TODO [L2][练习] 手写 String 到目标类型的分发逻辑——先查 converterTable 命中则 apply；未命中且目标为枚举则 Enum.valueOf；两者都不行则抛出 IllegalArgumentException（含目标类型名）；写对标志：能正确转换 int/boolean/enum，非法字符串抛异常。
    @SuppressWarnings("unchecked")
    private <T> T convertFromString(String value, Class<T> targetType) {
        Function<String, Object> converter = converterTable.get(targetType);
        if (converter != null) {
            return (T) converter.apply(value);
        }

        // TODO [L1][练习] 手写枚举解析分支——targetType.isEnum() 时用 Enum.valueOf((Class<Enum>) targetType, value) 转换，并捕获 IllegalArgumentException 抛出含有效枚举值列表的友好异常；写对标志：非法枚举值抛出的异常信息中包含 targetType.getEnumConstants()。
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

    // TODO [L2][练习] 手写自定义类型转换器的注册与接入——在 registerConverter 中把 (targetType, converter) 放入 customConverters，并在 convert 流程里当 converterTable 未命中时优先查 customConverters 并委托其 convert；写对标志：注册 LocalTime 转换器后，convert("12:30", LocalTime.class) 返回对应 LocalTime。
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

    // TODO [L3][优化-工厂方法] 每种基本类型的 String 到对象转换目前用 Map + lambda 硬编码。可为每种类型定义一个转换器工厂方法（如 IntegerConverter.create() / BooleanConverter.create()），由工厂统一注册进 converterTable，便于按类型替换、扩展与单测。；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
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
