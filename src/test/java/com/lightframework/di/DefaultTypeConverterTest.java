package com.lightframework.di;

import com.lightframework.di.core.DefaultTypeConverter;
import com.lightframework.di.core.TypeConverter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultTypeConverterTest {

    enum Color { RED, GREEN, BLUE }

    private final DefaultTypeConverter converter = new DefaultTypeConverter();

    @Test
    void convertStringToInt() throws Exception {
        // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿
        assertEquals(Integer.valueOf(123), converter.convert("123", Integer.class));
        assertEquals(Integer.valueOf(123), converter.convert("123", Integer.TYPE));
    }

    @Test
    void convertStringToBoolean() throws Exception {
        assertTrue((Boolean) converter.convert("true", Boolean.class));
        assertFalse((Boolean) converter.convert("false", Boolean.TYPE));
    }

    @Test
    void convertStringToDouble() throws Exception {
        assertEquals(3.14, (Double) converter.convert("3.14", Double.class), 1e-9);
    }

    @Test
    void convertEnum() throws Exception {
        // 验证 TODO[L1] 练习目标：枚举解析分支
        assertEquals(Color.GREEN, converter.convert("GREEN", Color.class));
    }

    @Test
    void convertUnsupportedThrows() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("abc", Integer.class));
    }

    @Test
    void supportsCoversKnownTypes() {
        assertTrue(converter.supports(Integer.class));
        assertTrue(converter.supports(String.class));
        assertTrue(converter.supports(Color.class));
        assertFalse(converter.supports(DefaultTypeConverterTest.class));
    }

    @Test
    void sameTypeReturnsSameInstance() throws Exception {
        // 验证 TODO[L1] 练习目标：源已是目标类型应直接短路返回同一对象
        Integer v = 5;
        assertSame(v, converter.convert(v, Number.class));
    }

    @Test
    void registerCustomConverter() throws Exception {
        // 验证 TODO[L2] 练习目标：注册自定义转换器后参与 convert 流程
        TypeConverter dateConverter = new TypeConverter() {
            public boolean supports(Class<?> t) { return t == LocalDate.class; }
            public <T> T convert(Object src, Class<T> t) {
                return t.cast(LocalDate.parse(src.toString()));
            }
        };
        converter.registerConverter(LocalDate.class, dateConverter);
        assertEquals(LocalDate.of(2020, 1, 1), converter.convert("2020-01-01", LocalDate.class));
    }
}
