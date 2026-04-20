package com.lightframework.ioc.context;

import com.lightframework.ioc.annotation.Component;
import com.lightframework.ioc.annotation.Scope;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassPathBeanDefinitionScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(ClassPathBeanDefinitionScanner.class);
    
    private final DefaultListableBeanFactory beanFactory;
    
    public ClassPathBeanDefinitionScanner(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }
    
    public int scan(String... basePackages) throws Exception {
        int count = 0;
        for (String basePackage : basePackages) {
            count += doScan(basePackage);
        }
        return count;
    }
    
    protected int doScan(String basePackage) throws Exception {
        String packagePath = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader()
            .getResources(packagePath);
        
        List<File> directories = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            directories.add(new File(resource.getFile()));
        }
        
        int count = 0;
        for (File directory : directories) {
            count += findAndRegisterComponents(directory, basePackage);
        }
        
        return count;
    }
    
    protected int findAndRegisterComponents(File directory, String basePackage) throws Exception {
        if (!directory.exists()) {
            return 0;
        }
        
        int count = 0;
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                count += findAndRegisterComponents(file, basePackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = basePackage + '.' + file.getName().substring(0, 
                    file.getName().length() - 6);
                
                try {
                    Class<?> clazz = Class.forName(className);
                    Component component = clazz.getAnnotation(Component.class);
                    
                    if (component != null && !clazz.isInterface() && !clazz.isAnnotation()) {
                        registerBeanDefinition(clazz, component);
                        count++;
                        logger.debug("Found component class: {}", className);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warn("Could not load class: {}", className);
                }
            }
        }
        
        return count;
    }
    
    protected void registerBeanDefinition(Class<?> beanClass, Component component) {
        String beanName = component.value();
        if (beanName.isEmpty()) {
            beanName = generateBeanName(beanClass);
        }
        
        BeanDefinition bd = new BeanDefinition(beanName, beanClass);
        
        Scope scope = beanClass.getAnnotation(Scope.class);
        if (scope != null) {
            bd.setScope(scope.value());
        }
        
        this.beanFactory.registerBeanDefinition(beanName, bd);
    }
    
    protected String generateBeanName(Class<?> beanClass) {
        String shortName = beanClass.getSimpleName();
        return shortName.substring(0, 1).toLowerCase() + shortName.substring(1);
    }
}