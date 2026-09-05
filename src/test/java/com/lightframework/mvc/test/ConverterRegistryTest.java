package com.lightframework.mvc.test;

import com.lightframework.mvc.convert.Converter;
import com.lightframework.mvc.convert.ConverterRegistry;
import com.lightframework.mvc.convert.StringToBooleanConverter;
import com.lightframework.mvc.convert.StringToIntegerConverter;
import com.lightframework.mvc.convert.StringToLongConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConverterRegistryTest {

    // 验证 TODO[L3][优化-工厂方法] 练习目标：用户手写实现后运行本测试应全绿（注册转换器后按目标类型转换）。
    @Test
    void convertsUsingRegisteredConverter() {
        ConverterRegistry registry = new ConverterRegistry();
        registry.addConverter(new StringToIntegerConverter());
        registry.addConverter(new StringToLongConverter());
        assertEquals(Integer.valueOf(123), registry.convert("123", Integer.class));
        assertEquals(Long.valueOf(456L), registry.convert("456", Long.class));
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（新增自定义 Converter 并注册生效）。
    @Test
    void customConverterCanBeRegistered() {
        ConverterRegistry registry = new ConverterRegistry();
        registry.addConverter(new Converter<String, String>() {
            @Override
            public String convert(String s) {
                return s.toUpperCase();
            }

            @Override
            public Class<String> getSourceType() {
                return String.class;
            }

            @Override
            public Class<String> getTargetType() {
                return String.class;
            }
        });
        assertEquals("ABC", registry.convert("abc", String.class));
    }

    @Test
    void findConverterReturnsMatchingConverter() {
        ConverterRegistry registry = new ConverterRegistry();
        registry.addConverter(new StringToBooleanConverter());
        assertNotNull(registry.findConverter(Boolean.class));
    }
}
