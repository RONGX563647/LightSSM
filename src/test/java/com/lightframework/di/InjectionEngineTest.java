package com.lightframework.di;

import com.lightframework.di.core.AnnotationInjectEntry;
import com.lightframework.di.core.DefaultTypeConverter;
import com.lightframework.di.core.DependencyContainer;
import com.lightframework.di.core.DependencyResolutionException;
import com.lightframework.di.core.FieldInjector;
import com.lightframework.di.core.InjectionEngine;
import com.lightframework.di.core.InjectionMetadata;
import com.lightframework.di.core.PlaceholderResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class InjectionEngineTest {

    // ===== 测试用 Bean =====
    @SuppressWarnings("unused")
    static class Dep { }

    @SuppressWarnings("unused")
    static class Service {
        private Dep resourceByName;
        private Dep autowiredByType;
        private Dep lazyField;
        private Dep optionalMissing;
        private int port;
        private Dep setterInjected;
        private Dep dual;

        public void setSetterInjected(Dep d) { this.setterInjected = d; }
    }

    // 循环依赖专用：A 依赖 B、B 依赖 A
    @SuppressWarnings("unused")
    static class NodeA { private NodeB b; }
    @SuppressWarnings("unused")
    static class NodeB { private NodeA a; }

    private static Field field(Class<?> clazz, String name) throws Exception {
        return clazz.getDeclaredField(name);
    }

    private static FieldInjector reflectInjector(Field f) {
        return (bean, value) -> { f.setAccessible(true); f.set(bean, value); };
    }

    // ===== @Resource 字段按名称注入 =====
    @Test
    void resourceFieldByName() throws Exception {
        // 验证 TODO[L2] 练习目标：@Resource 先按 name 查找
        DependencyContainer container = mock(DependencyContainer.class);
        Dep dep = new Dep();
        when(container.containsBean("dep")).thenReturn(true);
        when(container.getBean("dep", Dep.class)).thenReturn(dep);

        Field f = field(Service.class, "resourceByName");
        InjectionMetadata md = new InjectionMetadata();
        md.resourceFields.add(f);
        md.resourceEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, true, null, "dep", null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertSame(dep, s.resourceByName);
    }

    // ===== @Resource 字段按类型回退 =====
    @Test
    void resourceFieldByTypeFallback() throws Exception {
        // 验证 TODO[L2] 练习目标：name 缺失时回退到按类型查找
        DependencyContainer container = mock(DependencyContainer.class);
        Dep dep = new Dep();
        when(container.containsBean(anyString())).thenReturn(false);
        when(container.getBean(Dep.class)).thenReturn(dep);

        Field f = field(Service.class, "resourceByName");
        InjectionMetadata md = new InjectionMetadata();
        md.resourceFields.add(f);
        md.resourceEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, true, null, null, null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertSame(dep, s.resourceByName);
    }

    // ===== @Autowired 字段按类型注入 =====
    @Test
    void autowiredFieldByType() throws Exception {
        // 验证 TODO[L2] 练习目标：@Autowired 按类型解析（含 required）
        DependencyContainer container = mock(DependencyContainer.class);
        Dep dep = new Dep();
        when(container.resolveDependencyWithGenerics(any(Field.class), any(), anyBoolean())).thenReturn(dep);

        Field f = field(Service.class, "autowiredByType");
        InjectionMetadata md = new InjectionMetadata();
        md.autowiredFields.add(f);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, true, null, null, null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertSame(dep, s.autowiredByType);
    }

    // ===== @Autowired 方法（setter）注入 =====
    @Test
    void autowiredMethodInjection() throws Exception {
        // 验证 TODO[L2] 练习目标：@Autowired setter 注入
        DependencyContainer container = mock(DependencyContainer.class);
        Dep dep = new Dep();
        when(container.resolveDependency(eq(Dep.class), anyString(), isNull(), eq(true))).thenReturn(dep);

        Method m = Service.class.getDeclaredMethod("setSetterInjected", Dep.class);
        InjectionMetadata md = new InjectionMetadata();
        md.autowiredMethods.add(m);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, (b, v) -> { }, null, null, true, null, null, null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertSame(dep, s.setterInjected);
    }

    // ===== @Value 占位符解析 + 类型转换 =====
    @Test
    void valueFieldInjection() throws Exception {
        // 验证 TODO[L1] 练习目标：@Value 占位符解析后转目标类型
        DependencyContainer container = mock(DependencyContainer.class);
        PlaceholderResolver resolver = mock(PlaceholderResolver.class);
        when(resolver.resolvePlaceholder("port")).thenReturn("8080");

        Field f = field(Service.class, "port");
        InjectionMetadata md = new InjectionMetadata();
        md.valueFields.add(f);
        md.valueEntries.add(new AnnotationInjectEntry(int.class, reflectInjector(f), f, null, true, null, null, "port"));

        Service s = new Service();
        new InjectionEngine(container, new DefaultTypeConverter(), resolver).injectAll("svc", s, md);
        assertEquals(8080, s.port);
    }

    @Test
    void valueFieldEmptyPlaceholderThrows() throws Exception {
        // 验证 TODO[L1] 练习目标：占位符为空时引擎应抛 DependencyResolutionException
        DependencyContainer container = mock(DependencyContainer.class);
        PlaceholderResolver resolver = mock(PlaceholderResolver.class);

        Field f = field(Service.class, "port");
        InjectionMetadata md = new InjectionMetadata();
        md.valueFields.add(f);
        md.valueEntries.add(new AnnotationInjectEntry(int.class, reflectInjector(f), f, null, true, null, null, ""));

        Service s = new Service();
        assertThrows(DependencyResolutionException.class,
                () -> new InjectionEngine(container, new DefaultTypeConverter(), resolver).injectAll("svc", s, md));
    }

    // ===== @Lazy 代理直接注入，跳过实时解析 =====
    @Test
    void lazyProxyInjectedWithoutResolution() throws Exception {
        // 验证 TODO[L1] 练习目标：@Lazy 代理分支直接注入代理对象
        DependencyContainer container = mock(DependencyContainer.class);
        Dep proxy = new Dep();
        Dep wrong = new Dep();
        when(container.resolveDependencyWithGenerics(any(), any(), anyBoolean())).thenReturn(wrong);

        Field f = field(Service.class, "lazyField");
        InjectionMetadata md = new InjectionMetadata();
        md.autowiredFields.add(f);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, true, proxy, null, null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertSame(proxy, s.lazyField);
        verify(container, never()).resolveDependencyWithGenerics(any(), any(), anyBoolean());
    }

    // ===== 跨注解去重：同字段 @Resource+@Autowired 只注入一次 =====
    @Test
    void dualAnnotationInjectsOnce() throws Exception {
        // 验证 TODO[L2] 练习目标：同字段不应被注入两次
        DependencyContainer container = mock(DependencyContainer.class);
        Dep dep = new Dep();
        when(container.containsBean(anyString())).thenReturn(false);
        when(container.getBean(Dep.class)).thenReturn(dep);
        when(container.resolveDependencyWithGenerics(any(), any(), anyBoolean())).thenReturn(dep);

        Field f = field(Service.class, "dual");
        int[] count = {0};
        FieldInjector counting = (bean, value) -> { f.setAccessible(true); f.set(bean, value); count[0]++; };

        InjectionMetadata md = new InjectionMetadata();
        md.resourceFields.add(f);
        md.resourceEntries.add(new AnnotationInjectEntry(Dep.class, counting, f, null, true, null, null, null));
        md.autowiredFields.add(f);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, counting, f, null, true, null, null, null));

        Service s = new Service();
        new InjectionEngine(container, null, null).injectAll("svc", s, md);
        assertEquals(1, count[0], "同字段不应被注入两次");
        assertSame(dep, s.dual);
    }

    // ===== @Autowired required=false 缺失不抛异常 =====
    @Test
    void optionalDependencyMissingNoThrow() throws Exception {
        // 验证 TODO[L1] 练习目标：可选依赖缺失时保持 null 且不抛异常
        DependencyContainer container = mock(DependencyContainer.class);
        when(container.resolveDependencyWithGenerics(any(), any(), anyBoolean())).thenReturn(null);

        Field f = field(Service.class, "optionalMissing");
        InjectionMetadata md = new InjectionMetadata();
        md.autowiredFields.add(f);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, false, null, null, null));

        Service s = new Service();
        assertDoesNotThrow(() -> new InjectionEngine(container, null, null).injectAll("svc", s, md));
        assertNull(s.optionalMissing);
    }

    // ===== @Autowired required=true 缺失抛异常 =====
    @Test
    void requiredDependencyMissingThrows() throws Exception {
        // 验证 TODO[L1] 练习目标：必需依赖缺失时应抛 DependencyResolutionException
        DependencyContainer container = mock(DependencyContainer.class);
        when(container.resolveDependencyWithGenerics(any(), any(), anyBoolean())).thenReturn(null);

        Field f = field(Service.class, "autowiredByType");
        InjectionMetadata md = new InjectionMetadata();
        md.autowiredFields.add(f);
        md.autowiredEntries.add(new AnnotationInjectEntry(Dep.class, reflectInjector(f), f, null, true, null, null, null));

        Service s = new Service();
        assertThrows(DependencyResolutionException.class,
                () -> new InjectionEngine(container, null, null).injectAll("svc", s, md));
    }

    // ===== 循环依赖：字段注入本身可完成（解析顺序由 ioc 负责） =====
    @Test
    void circularDependencyFieldInjection() throws Exception {
        // 验证：A↔B 互相依赖时，注入引擎能正确把双方实例填入对应字段
        DependencyContainer container = mock(DependencyContainer.class);
        NodeA a = new NodeA();
        NodeB b = new NodeB();
        when(container.resolveDependencyWithGenerics(any(Field.class), any(), anyBoolean()))
                .thenAnswer(inv -> {
                    Field f = inv.getArgument(0);
                    return "b".equals(f.getName()) ? b : a;
                });

        Field fa = field(NodeA.class, "b");
        InjectionMetadata mdA = new InjectionMetadata();
        mdA.autowiredFields.add(fa);
        mdA.autowiredEntries.add(new AnnotationInjectEntry(NodeB.class, reflectInjector(fa), fa, null, true, null, null, null));

        Field fb = field(NodeB.class, "a");
        InjectionMetadata mdB = new InjectionMetadata();
        mdB.autowiredFields.add(fb);
        mdB.autowiredEntries.add(new AnnotationInjectEntry(NodeA.class, reflectInjector(fb), fb, null, true, null, null, null));

        new InjectionEngine(container, null, null).injectAll("a", a, mdA);
        new InjectionEngine(container, null, null).injectAll("b", b, mdB);

        assertSame(b, a.b);
        assertSame(a, b.a);
    }
}
