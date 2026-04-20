package com.lightframework.ioc.context;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.Scope;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.BeanPostProcessor;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class AnnotationConfigApplicationContext implements ApplicationContext {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationConfigApplicationContext.class);
    
    private final DefaultListableBeanFactory beanFactory;
    
    private final List<Class<?>> componentClasses = new ArrayList<>();
    
    private String id = "light-framework-context";
    private String displayName = "AnnotationConfigApplicationContext";
    private long startupDate;
    private boolean active = false;
    
    public AnnotationConfigApplicationContext() {
        this.beanFactory = new DefaultListableBeanFactory();
    }
    
    public AnnotationConfigApplicationContext(Class<?>... componentClasses) throws Exception {
        this();
        register(componentClasses);
        refresh();
    }
    
    public AnnotationConfigApplicationContext(String... basePackages) throws Exception {
        this();
        scan(basePackages);
        refresh();
    }
    
    public void register(Class<?>... componentClasses) {
        for (Class<?> componentClass : componentClasses) {
            this.componentClasses.add(componentClass);
        }
    }
    
    public void scan(String... basePackages) throws Exception {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.beanFactory);
        int count = scanner.scan(basePackages);
        logger.info("Scanned {} component classes from packages: {}", count, basePackages);
    }
    
    @Override
    public void refresh() throws Exception {
        this.startupDate = System.currentTimeMillis();
        this.active = true;
        
        registerBeanDefinitions();
        
        registerBeanPostProcessors();
        
        this.beanFactory.preInstantiateSingletons();
        
        logger.info("ApplicationContext refreshed successfully, {} beans instantiated", 
            this.beanFactory.getBeanDefinitionCount());
    }
    
    protected void registerBeanDefinitions() throws Exception {
        for (Class<?> componentClass : this.componentClasses) {
            registerComponent(componentClass);
        }
    }
    
    protected void registerComponent(Class<?> componentClass) {
        Component component = componentClass.getAnnotation(Component.class);
        if (component == null) {
            throw new IllegalArgumentException("Class " + componentClass.getName() + 
                " is not annotated with @Component");
        }
        
        String beanName = component.value();
        if (beanName.isEmpty()) {
            beanName = generateBeanName(componentClass);
        }
        
        BeanDefinition bd = new BeanDefinition(beanName, componentClass);
        
        Scope scope = componentClass.getAnnotation(Scope.class);
        if (scope != null) {
            bd.setScope(scope.value());
        }
        
        this.beanFactory.registerBeanDefinition(beanName, bd);
        logger.debug("Registered bean definition: {} -> {}", beanName, componentClass.getName());
    }
    
    protected String generateBeanName(Class<?> beanClass) {
        String shortName = beanClass.getSimpleName();
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }
    
    protected void registerBeanPostProcessors() throws Exception {
        String[] postProcessorNames = this.beanFactory.getBeanNamesForType(BeanPostProcessor.class);
        for (String ppName : postProcessorNames) {
            BeanPostProcessor pp = this.beanFactory.getBean(ppName, BeanPostProcessor.class);
            this.beanFactory.addBeanPostProcessor(pp);
        }
    }
    
    @Override
    public Object getBean(String name) throws Exception {
        return this.beanFactory.getBean(name);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws Exception {
        return this.beanFactory.getBean(name, requiredType);
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        return this.beanFactory.getBean(requiredType);
    }
    
    @Override
    public boolean containsBean(String name) {
        return this.beanFactory.containsBean(name);
    }
    
    @Override
    public boolean isSingleton(String name) {
        return this.beanFactory.isSingleton(name);
    }
    
    @Override
    public boolean isPrototype(String name) {
        return this.beanFactory.isPrototype(name);
    }
    
    @Override
    public Class<?> getType(String name) {
        return this.beanFactory.getType(name);
    }
    
    @Override
    public String[] getAliases(String name) {
        return this.beanFactory.getAliases(name);
    }
    
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanFactory.containsBeanDefinition(beanName);
    }
    
    @Override
    public int getBeanDefinitionCount() {
        return this.beanFactory.getBeanDefinitionCount();
    }
    
    @Override
    public String[] getBeanDefinitionNames() {
        return this.beanFactory.getBeanDefinitionNames();
    }
    
    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        return this.beanFactory.getBeanNamesForType(type);
    }
    
    @Override
    public <T> List<T> getBeansOfType(Class<T> type) throws Exception {
        return this.beanFactory.getBeansOfType(type);
    }
    
    @Override
    public <T> T getPrimaryBean(Class<T> type) throws Exception {
        return this.beanFactory.getPrimaryBean(type);
    }
    
    @Override
    public String getId() {
        return this.id;
    }
    
    @Override
    public String getApplicationName() {
        return "light-framework";
    }
    
    @Override
    public String getDisplayName() {
        return this.displayName;
    }
    
    @Override
    public long getStartupDate() {
        return this.startupDate;
    }
    
    @Override
    public ApplicationContext getParent() {
        return null;
    }
    
    @Override
    public void close() {
        this.active = false;
        logger.info("ApplicationContext closed");
    }
    
    @Override
    public boolean isActive() {
        return this.active;
    }
    
    public DefaultListableBeanFactory getBeanFactory() {
        return this.beanFactory;
    }
}