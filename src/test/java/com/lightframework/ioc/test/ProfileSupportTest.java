package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Profile;
import com.lightframework.ioc.context.AnnotationConfigApplicationContext;
import com.lightframework.ioc.core.Environment;
import com.lightframework.ioc.core.StandardEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test beans for Profile support.
 */
@Component
class DefaultProfileBean {
    public String getName() { return "DefaultProfileBean"; }
}

@Component
@Profile("dev")
class DevProfileBean {
    public String getName() { return "DevProfileBean"; }
}

@Component
@Profile("prod")
class ProdProfileBean {
    public String getName() { return "ProdProfileBean"; }
}

@Component
@Profile({"dev", "test"})
class MultiProfileBean {
    public String getName() { return "MultiProfileBean"; }
}

@Component
class NoProfileBean {
    public String getName() { return "NoProfileBean"; }
}

/**
 * Test class for @Profile support.
 */
public class ProfileSupportTest {
    
    private String originalProfilesProperty;
    
    @BeforeEach
    void setUp() {
        originalProfilesProperty = System.getProperty("light.profiles.active");
        System.clearProperty("light.profiles.active");
    }
    
    @AfterEach
    void tearDown() {
        if (originalProfilesProperty != null) {
            System.setProperty("light.profiles.active", originalProfilesProperty);
        } else {
            System.clearProperty("light.profiles.active");
        }
    }
    
    @Test
    public void testDefaultProfile() throws Exception {
        // Default profile is active when no system property is set
        StandardEnvironment env = new StandardEnvironment();
        assertTrue(env.acceptsProfiles("default"));
        
        // Use explicit registration to test Profile filtering
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("default");
        ctx.register(DefaultProfileBean.class, NoProfileBean.class);
        ctx.refresh();
        
        assertTrue(ctx.containsBean("defaultProfileBean"));
        assertTrue(ctx.containsBean("noProfileBean"));
        
        ctx.close();
    }
    
    @Test
    public void testMatchingProfile() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("dev");
        ctx.register(DevProfileBean.class, NoProfileBean.class, ProdProfileBean.class);
        ctx.refresh();
        
        assertTrue(ctx.containsBean("devProfileBean"), "DevProfileBean should be registered with 'dev' profile active");
        assertTrue(ctx.containsBean("noProfileBean"), "NoProfileBean should always be registered");
        assertFalse(ctx.containsBean("prodProfileBean"), "ProdProfileBean should NOT be registered with 'dev' profile active");
        
        ctx.close();
    }
    
    @Test
    public void testNonMatchingProfile() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("prod");
        ctx.register(DevProfileBean.class, NoProfileBean.class);
        ctx.refresh();
        
        assertFalse(ctx.containsBean("devProfileBean"), "DevProfileBean should NOT be registered with 'prod' profile active");
        assertTrue(ctx.containsBean("noProfileBean"), "NoProfileBean should always be registered");
        
        ctx.close();
    }
    
    @Test
    public void testMultipleActiveProfiles() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("dev", "prod");
        ctx.register(DevProfileBean.class, ProdProfileBean.class, NoProfileBean.class);
        ctx.refresh();
        
        assertTrue(ctx.containsBean("devProfileBean"), "DevProfileBean should be registered");
        assertTrue(ctx.containsBean("prodProfileBean"), "ProdProfileBean should be registered");
        assertTrue(ctx.containsBean("noProfileBean"), "NoProfileBean should always be registered");
        
        ctx.close();
    }
    
    @Test
    public void testMultiProfileAnnotation() throws Exception {
        // Test bean with @Profile({"dev", "test"}) - should match if either is active
        AnnotationConfigApplicationContext ctxDev = new AnnotationConfigApplicationContext();
        ctxDev.getEnvironment().setActiveProfiles("dev");
        ctxDev.register(MultiProfileBean.class, NoProfileBean.class);
        ctxDev.refresh();
        
        assertTrue(ctxDev.containsBean("multiProfileBean"), "MultiProfileBean should be registered with 'dev' active");
        ctxDev.close();
        
        AnnotationConfigApplicationContext ctxTest = new AnnotationConfigApplicationContext();
        ctxTest.getEnvironment().setActiveProfiles("test");
        ctxTest.register(MultiProfileBean.class, NoProfileBean.class);
        ctxTest.refresh();
        
        assertTrue(ctxTest.containsBean("multiProfileBean"), "MultiProfileBean should be registered with 'test' active");
        ctxTest.close();
        
        AnnotationConfigApplicationContext ctxProd = new AnnotationConfigApplicationContext();
        ctxProd.getEnvironment().setActiveProfiles("prod");
        ctxProd.register(MultiProfileBean.class, NoProfileBean.class);
        ctxProd.refresh();
        
        assertFalse(ctxProd.containsBean("multiProfileBean"), "MultiProfileBean should NOT be registered with 'prod' active");
        ctxProd.close();
    }
    
    @Test
    public void testNoProfileAlwaysRegistered() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("someRandomProfile");
        ctx.register(NoProfileBean.class);
        ctx.refresh();
        
        assertTrue(ctx.containsBean("noProfileBean"), "Bean without @Profile should always be registered");
        
        ctx.close();
    }
    
    @Test
    public void testEnvironmentGetActiveProfiles() {
        StandardEnvironment env = new StandardEnvironment();
        String[] profiles = env.getActiveProfiles();
        assertEquals(1, profiles.length);
        assertEquals("default", profiles[0]);
    }
    
    @Test
    public void testEnvironmentSetActiveProfiles() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("dev", "test", "prod");
        
        String[] profiles = env.getActiveProfiles();
        assertEquals(3, profiles.length);
        assertTrue(env.acceptsProfiles("dev"));
        assertTrue(env.acceptsProfiles("test"));
        assertTrue(env.acceptsProfiles("prod"));
        assertFalse(env.acceptsProfiles("staging"));
    }
    
    @Test
    public void testEnvironmentAddActiveProfile() {
        StandardEnvironment env = new StandardEnvironment();
        env.addActiveProfile("dev");
        
        assertTrue(env.acceptsProfiles("dev"));
        assertTrue(env.acceptsProfiles("default"));
    }
    
    @Test
    public void testEnvironmentAcceptsProfilesWithNull() {
        StandardEnvironment env = new StandardEnvironment();
        assertTrue(env.acceptsProfiles(null));
        assertTrue(env.acceptsProfiles());
    }
    
    @Test
    public void testEnvironmentGetDefaultProfile() {
        StandardEnvironment env = new StandardEnvironment();
        assertEquals("default", env.getDefaultProfile());
    }
    
    @Test
    public void testEnvironmentAcceptsProfilesWithEmptyArray() {
        StandardEnvironment env = new StandardEnvironment();
        assertTrue(env.acceptsProfiles(new String[0]));
    }
    
    @Test
    public void testEnvironmentAcceptsProfilesWithMultipleArgs() {
        StandardEnvironment env = new StandardEnvironment();
        env.setActiveProfiles("dev");
        
        // At least one profile matches
        assertTrue(env.acceptsProfiles("test", "dev", "prod"));
        // None matches
        assertFalse(env.acceptsProfiles("test", "prod"));
    }
    
    @Test
    public void testBeanCountWithProfileFiltering() throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("dev");
        ctx.register(DefaultProfileBean.class, DevProfileBean.class, ProdProfileBean.class, 
                     MultiProfileBean.class, NoProfileBean.class);
        ctx.refresh();
        
        // dev profile active: DefaultProfileBean (default), DevProfileBean (dev), 
        // MultiProfileBean (dev|test), NoProfileBean (no profile)
        // ProdProfileBean should be excluded
        // Plus 4 SPI auto-configuration classes (Hutool, Jackson, Caffeine, MyBatis)
        assertEquals(8, ctx.getBeanDefinitionCount(), 
            "Expected 8 beans with 'dev' profile active");
        
        ctx.close();
    }
}
