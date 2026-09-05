package com.lightframework.di;

import com.lightframework.di.core.AnnotationInjectEntry;
import com.lightframework.di.core.FieldInjector;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationInjectEntryTest {

    @SuppressWarnings("unused")
    static class Holder {
        private Object field;
    }

    private final FieldInjector noop = (b, v) -> { };

    private static Field field() throws Exception {
        return Holder.class.getDeclaredField("field");
    }

    @Test
    void convenienceConstructorDerivesFieldName() throws Exception {
        // 验证 TODO[L1] 练习目标：便捷构造函数正确设置默认值并推导 fieldName
        Field f = field();
        AnnotationInjectEntry e = new AnnotationInjectEntry(Object.class, noop, f);
        assertEquals("field", e.fieldName);
        assertEquals(Object.class, e.type);
        assertTrue(e.required);
        assertNull(e.qualifier);
        assertNull(e.lazyProxy);
    }

    @Test
    void fullConstructorPreservesAll() throws Exception {
        // 验证 TODO[L3][优化-建造者] 练习目标：全参构造正确保存所有预计算字段
        Field f = field();
        Object proxy = new Object();
        AnnotationInjectEntry e = new AnnotationInjectEntry(
                Object.class, noop, f, "q", false, proxy, "res", "${x}");
        assertEquals("q", e.qualifier);
        assertFalse(e.required);
        assertSame(proxy, e.lazyProxy);
        assertEquals("res", e.resourceName);
        assertEquals("${x}", e.placeholder);
    }

    @Test
    void fieldNameNullWhenFieldNull() throws Exception {
        // 验证 TODO[L2] 练习目标：field 为 null 时 fieldName 应为 null
        AnnotationInjectEntry e = new AnnotationInjectEntry(Object.class, noop);
        assertNull(e.fieldName);
    }
}
