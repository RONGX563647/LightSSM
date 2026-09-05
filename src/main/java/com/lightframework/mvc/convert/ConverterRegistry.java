package com.lightframework.mvc.convert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConverterRegistry {
    // TODO [L3][优化-工厂方法] 当前转换器由调用方手动 addConverter 注册；可用工厂方法按 source/target 类型自动发现（SPI 扫描或注解标记），；写对标志：按工厂方法模式完成实现，新增单测覆盖“按类型/参数创建不同产品”的主路径与一条异常路径，断言返回对象类型与属性正确。
    //   在 ConverterRegistry 构造时批量注册，新增 Converter 实现即自动生效、无需改动注册处。
    private final List<Converter<String, ?>> converters = new CopyOnWriteArrayList<>();

    public void addConverter(Converter<String, ?> converter) {
        // TODO [L1][练习] 仿照 StringToIntegerConverter，新增一个 StringToBigDecimalConverter（或 StringToLocalDateConverter）实现 Converter 接口，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并在 RequestMappingHandlerAdapter 构造器里 addConverter 注册。验收标准：@RequestParam BigDecimal price 能被正确解析。
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
