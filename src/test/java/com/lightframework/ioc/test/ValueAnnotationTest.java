package com.lightframework.ioc.test;

import com.lightframework.di.annotation.Component;
import com.lightframework.di.annotation.Value;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.PropertyPlaceholderConfigurer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for @Value annotation and PropertyPlaceholderConfigurer.
 */
public class ValueAnnotationTest {

    private static final String TEST_PROPERTIES_FILE = "test-application.properties";
    private String savedSystemProp;

    @Component("valueTestBean")
    public static class ValueTestBean {
        @Value("${app.name}")
        private String appName;

        @Value("${app.port:8080}")
        private int port;

        @Value("${app.timeout}")
        private long timeout;

        @Value("${app.enabled}")
        private boolean enabled;

        @Value("${app.rate}")
        private double rate;

        @Value("${app.missing.key:not-found}")
        private String withDefaultValue;

        // Getters
        public String getAppName() { return appName; }
        public int getPort() { return port; }
        public long getTimeout() { return timeout; }
        public boolean isEnabled() { return enabled; }
        public double getRate() { return rate; }
        public String getWithDefaultValue() { return withDefaultValue; }
    }

    @Component("simpleValueBean")
    public static class SimpleValueBean {
        @Value("${simple.key}")
        private String value;

        public String getValue() { return value; }
    }

    @Component("defaultValueBean")
    public static class DefaultValueBean {
        @Value("${app.port:8080}")
        private int port;

        @Value("${app.max.connections:100}")
        private int maxConnections;

        public int getPort() { return port; }
        public int getMaxConnections() { return maxConnections; }
    }

    @BeforeEach
    public void setUp() {
        // Save any existing system property that we'll use in tests
        savedSystemProp = System.getProperty("app.fromSystem");
    }

    @AfterEach
    public void tearDown() {
        // Restore original system property
        if (savedSystemProp != null) {
            System.setProperty("app.fromSystem", savedSystemProp);
        } else {
            System.clearProperty("app.fromSystem");
        }
    }

    @Test
    public void testBasicPlaceholderInjection() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("simpleValueBean", SimpleValueBean.class);
        factory.registerBeanDefinition("simpleValueBean", bd);

        SimpleValueBean bean = factory.getBean("simpleValueBean", SimpleValueBean.class);
        assertEquals("hello-world", bean.getValue());
    }

    @Test
    public void testPlaceholderWithDefaultValue() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        ValueTestBean bean = factory.getBean("valueTestBean", ValueTestBean.class);

        // app.missing.key is not in properties, so default value should be used
        assertEquals("not-found", bean.getWithDefaultValue());
    }

    @Test
    public void testTypeConversionInt() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        ValueTestBean bean = factory.getBean("valueTestBean", ValueTestBean.class);

        // app.port is in properties as 9090
        assertEquals(9090, bean.getPort());
    }

    @Test
    public void testTypeConversionIntWithDefault() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        // Do NOT load any properties file - port uses default value
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("defaultValueBean", DefaultValueBean.class);
        factory.registerBeanDefinition("defaultValueBean", bd);

        DefaultValueBean bean = factory.getBean("defaultValueBean", DefaultValueBean.class);

        // app.port not in properties, default 8080 should be used
        assertEquals(8080, bean.getPort());
        // app.max.connections not in properties, default 100 should be used
        assertEquals(100, bean.getMaxConnections());
    }

    @Test
    public void testTypeConversionLong() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        ValueTestBean bean = factory.getBean("valueTestBean", ValueTestBean.class);

        assertEquals(5000L, bean.getTimeout());
    }

    @Test
    public void testTypeConversionBoolean() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        ValueTestBean bean = factory.getBean("valueTestBean", ValueTestBean.class);

        assertTrue(bean.isEnabled());
    }

    @Test
    public void testTypeConversionDouble() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        ValueTestBean bean = factory.getBean("valueTestBean", ValueTestBean.class);

        assertEquals(3.14, bean.getRate(), 0.001);
    }

    @Test
    public void testPlaceholderNotFoundThrowsException() throws Exception {
        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        // Register a bean that references a key that doesn't exist and has no default
        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);
        configurer.postProcessBeanFactory(factory);

        // app.name is NOT in properties and has NO default value
        assertThrows(IllegalArgumentException.class, () -> {
            factory.getBean("valueTestBean", ValueTestBean.class);
        });
    }

    @Test
    public void testSystemPropertyFallback() throws Exception {
        // Set a system property
        System.setProperty("app.fromSystem", "system-value");

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        // Create a bean that uses the system property key
        BeanDefinition bd = new BeanDefinition("valueTestBean", ValueTestBean.class);
        factory.registerBeanDefinition("valueTestBean", bd);

        // Manually verify configurer resolves system property
        String resolved = configurer.getProperty("app.fromSystem");
        assertEquals("system-value", resolved);
    }

    @Test
    public void testPropertiesOverrideSystemProperties() throws Exception {
        // Set a system property for a key NOT in the properties file
        System.setProperty("app.not.in.file", "system-fallback-value");

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocations(new String[]{TEST_PROPERTIES_FILE});
        configurer.postProcessBeanFactory(factory);

        // Key not in properties file should fall back to system property
        String resolved = configurer.getProperty("app.not.in.file");
        assertEquals("system-fallback-value", resolved);

        // Key in properties file should return properties file value, not system property
        // Set system property for a key that IS in the properties file
        System.setProperty("app.name", "system-name-override");
        String resolvedName = configurer.getProperty("app.name");
        // Properties file value takes precedence
        assertEquals("test-app", resolvedName);

        // Clean up
        System.clearProperty("app.not.in.file");
        System.clearProperty("app.name");
    }
}
