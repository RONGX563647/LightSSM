package com.lightframework.di;

import com.lightframework.di.core.InjectionMetadata;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class InjectionMetadataTest {

    @SuppressWarnings("unused")
    static class Sample {
        private Object a;
        private Object b;
        public void setA(Object o) { }
    }

    private static Field fieldA() throws Exception {
        return Sample.class.getDeclaredField("a");
    }

    private static Field fieldB() throws Exception {
        return Sample.class.getDeclaredField("b");
    }

    private static Method methodA() throws Exception {
        return Sample.class.getDeclaredMethod("setA", Object.class);
    }

    @Test
    void fieldDedup() throws Exception {
        // 验证 TODO[L1] 练习目标：字段级去重 mark/is
        InjectionMetadata md = new InjectionMetadata();
        Field fa = fieldA();
        assertFalse(md.isFieldInjected(fa));
        md.markFieldInjected(fa);
        assertTrue(md.isFieldInjected(fa));
        assertFalse(md.isFieldInjected(fieldB()));
    }

    @Test
    void methodDedup() throws Exception {
        // 验证 TODO[L1] 练习目标：方法级去重 mark/is
        InjectionMetadata md = new InjectionMetadata();
        Method m = methodA();
        assertFalse(md.isMethodInjected(m));
        md.markMethodInjected(m);
        assertTrue(md.isMethodInjected(m));
    }

    @Test
    void dualAnnotationConsideredInjected() throws Exception {
        // 验证 TODO[L2] 练习目标：同字段被 @Resource+@Autowired 标记时只注入一次
        InjectionMetadata md = new InjectionMetadata();
        Field fa = fieldA();
        md.resourceFields.add(fa);
        md.autowiredFields.add(fa);
        md.markFieldInjected(fa);
        assertTrue(md.isFieldInjected(fa));
    }
}
