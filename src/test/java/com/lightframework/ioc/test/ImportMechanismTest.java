package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Import;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.ImportBeanDefinitionRegistrar;
import com.lightframework.ioc.core.ImportSelector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @Import 机制测试用例。
 * 测试场景：
 * 1. @Import 普通配置类
 * 2. @Import ImportSelector 实现类
 * 3. @Import ImportBeanDefinitionRegistrar 实现类
 * 4. 链式 @Import（A import B, B import C）
 * 5. 混合使用多种 Import 方式
 */

// ========== 普通配置类 ==========

@Component("importedNormalBean1")
class ImportedNormalBean1 {
    public String getName() {
        return "ImportedNormalBean1";
    }
}

@Component("importedNormalBean2")
class ImportedNormalBean2 {
    public String getName() {
        return "ImportedNormalBean2";
    }
}

// ========== 配置类 A：@Import 普通配置类 ==========

@Import({ImportedNormalBean1.class})
@Component("configWithNormalImport")
class ConfigWithNormalImport {
}

// ========== ImportSelector 实现类 ==========

class TestImportSelector implements ImportSelector {
    @Override
    public String[] selectImports() {
        return new String[]{
            "com.lightframework.ioc.test.ImportedNormalBean1",
            "com.lightframework.ioc.test.ImportedNormalBean2"
        };
    }
}

// ========== 配置类 B：@Import ImportSelector ==========

@Import(TestImportSelector.class)
@Component("configWithSelectorImport")
class ConfigWithSelectorImport {
}

// ========== ImportBeanDefinitionRegistrar 实现类 ==========

class TestRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(DefaultListableBeanFactory registry) {
        // 直接注册两个 BeanDefinition
        BeanDefinition bd1 = new BeanDefinition("registrarBean1", RegistrarBean1.class);
        registry.registerBeanDefinition("registrarBean1", bd1);
        
        BeanDefinition bd2 = new BeanDefinition("registrarBean2", RegistrarBean2.class);
        registry.registerBeanDefinition("registrarBean2", bd2);
    }
}

@Component
class RegistrarBean1 {
    public String getName() {
        return "RegistrarBean1";
    }
}

@Component
class RegistrarBean2 {
    public String getName() {
        return "RegistrarBean2";
    }
}

// ========== 配置类 C：@Import ImportBeanDefinitionRegistrar ==========

@Import(TestRegistrar.class)
@Component("configWithRegistrarImport")
class ConfigWithRegistrarImport {
}

// ========== 链式 @Import：A import B, B import C ==========

@Component("chainLevel3Bean")
class ChainLevel3Bean {
    public String getName() {
        return "ChainLevel3Bean";
    }
}

@Import(ChainLevel3Bean.class)
@Component("chainLevel2Config")
class ChainLevel2Config {
}

@Import(ChainLevel2Config.class)
@Component("chainLevel1Config")
class ChainLevel1Config {
}

// ========== 混合使用：普通 + Selector + Registrar ==========

@Component("mixedServiceA")
class MixedServiceA {
    public String getName() {
        return "MixedServiceA";
    }
}

@Component("mixedServiceB")
class MixedServiceB {
    public String getName() {
        return "MixedServiceB";
    }
}

class MixedImportSelector implements ImportSelector {
    @Override
    public String[] selectImports() {
        return new String[]{"com.lightframework.ioc.test.MixedServiceA"};
    }
}

class MixedRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(DefaultListableBeanFactory registry) {
        BeanDefinition bd = new BeanDefinition("mixedRegistrarBean", MixedRegistrarBean.class);
        registry.registerBeanDefinition("mixedRegistrarBean", bd);
    }
}

@Component
class MixedRegistrarBean {
    public String getName() {
        return "MixedRegistrarBean";
    }
}

@Import({MixedServiceB.class, MixedImportSelector.class, MixedRegistrar.class})
@Component("mixedConfig")
class MixedConfig {
}

// ========== 测试用例 ==========

public class ImportMechanismTest {

    @Test
    public void testImportNormalClass() throws Exception {
        // 测试 @Import 普通配置类
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithNormalImport.class);
        
        assertTrue(ctx.containsBean("importedNormalBean1"), "Should contain importedNormalBean1");
        
        ImportedNormalBean1 bean = ctx.getBean("importedNormalBean1", ImportedNormalBean1.class);
        assertNotNull(bean);
        assertEquals("ImportedNormalBean1", bean.getName());
        
        ctx.close();
    }

    @Test
    public void testImportSelector() throws Exception {
        // 测试 @Import ImportSelector 实现类
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithSelectorImport.class);
        
        assertTrue(ctx.containsBean("importedNormalBean1"), "Should contain importedNormalBean1 via Selector");
        assertTrue(ctx.containsBean("importedNormalBean2"), "Should contain importedNormalBean2 via Selector");
        
        ImportedNormalBean1 bean1 = ctx.getBean("importedNormalBean1", ImportedNormalBean1.class);
        ImportedNormalBean2 bean2 = ctx.getBean("importedNormalBean2", ImportedNormalBean2.class);
        
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertEquals("ImportedNormalBean1", bean1.getName());
        assertEquals("ImportedNormalBean2", bean2.getName());
        
        ctx.close();
    }

    @Test
    public void testImportBeanDefinitionRegistrar() throws Exception {
        // 测试 @Import ImportBeanDefinitionRegistrar 实现类
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ConfigWithRegistrarImport.class);
        
        assertTrue(ctx.containsBean("registrarBean1"), "Should contain registrarBean1");
        assertTrue(ctx.containsBean("registrarBean2"), "Should contain registrarBean2");
        
        RegistrarBean1 bean1 = ctx.getBean("registrarBean1", RegistrarBean1.class);
        RegistrarBean2 bean2 = ctx.getBean("registrarBean2", RegistrarBean2.class);
        
        assertNotNull(bean1);
        assertNotNull(bean2);
        assertEquals("RegistrarBean1", bean1.getName());
        assertEquals("RegistrarBean2", bean2.getName());
        
        ctx.close();
    }

    @Test
    public void testChainedImport() throws Exception {
        // 测试链式 @Import（A import B, B import C）
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(ChainLevel1Config.class);
        
        // Level 1 配置类
        assertTrue(ctx.containsBean("chainLevel1Config"), "Should contain chainLevel1Config");
        // Level 2 配置类（被 Level 1 import）
        assertTrue(ctx.containsBean("chainLevel2Config"), "Should contain chainLevel2Config via chained import");
        // Level 3 Bean（被 Level 2 import）
        assertTrue(ctx.containsBean("chainLevel3Bean"), "Should contain chainLevel3Bean via chained import");
        
        ChainLevel3Bean bean = ctx.getBean("chainLevel3Bean", ChainLevel3Bean.class);
        assertNotNull(bean);
        assertEquals("ChainLevel3Bean", bean.getName());
        
        ctx.close();
    }

    @Test
    public void testMixedImports() throws Exception {
        // 测试混合使用多种 Import 方式
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(MixedConfig.class);
        
        // @Import 普通配置类
        assertTrue(ctx.containsBean("mixedServiceB"), "Should contain mixedServiceB via normal import");
        // @Import ImportSelector
        assertTrue(ctx.containsBean("mixedServiceA"), "Should contain mixedServiceA via Selector");
        // @Import ImportBeanDefinitionRegistrar
        assertTrue(ctx.containsBean("mixedRegistrarBean"), "Should contain mixedRegistrarBean via Registrar");
        
        MixedServiceA serviceA = ctx.getBean("mixedServiceA", MixedServiceA.class);
        MixedServiceB serviceB = ctx.getBean("mixedServiceB", MixedServiceB.class);
        MixedRegistrarBean registrarBean = ctx.getBean("mixedRegistrarBean", MixedRegistrarBean.class);
        
        assertNotNull(serviceA);
        assertNotNull(serviceB);
        assertNotNull(registrarBean);
        assertEquals("MixedServiceA", serviceA.getName());
        assertEquals("MixedServiceB", serviceB.getName());
        assertEquals("MixedRegistrarBean", registrarBean.getName());
        
        ctx.close();
    }
}
