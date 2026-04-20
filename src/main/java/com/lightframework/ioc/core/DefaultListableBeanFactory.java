package com.lightframework.ioc.core;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.exception.BeanCreationException;
import com.lightframework.ioc.exception.BeanCurrentlyInCreationException;
import com.lightframework.ioc.exception.NoSuchBeanDefinitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultListableBeanFactory implements ListableBeanFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultListableBeanFactory.class);
    
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);
    
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(16);
    
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>(16));
    
    private final Set<String> createdBeanNames = Collections.newSetFromMap(new ConcurrentHashMap<>(256));
    
    private final List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();
    
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        if (beanName == null || beanDefinition == null) {
            throw new IllegalArgumentException("beanName and beanDefinition must not be null");
        }
        this.beanDefinitionMap.put(beanName, beanDefinition);
        logger.debug("Registered bean definition: {}", beanName);
    }
    
    public void addBeanPostProcessor(BeanPostProcessor beanPostProcessor) {
        this.beanPostProcessors.remove(beanPostProcessor);
        this.beanPostProcessors.add(beanPostProcessor);
    }
    
    @Override
    public Object getBean(String name) throws Exception {
        return doGetBean(name, null, null);
    }
    
    @Override
    public <T> T getBean(String name, Class<T> requiredType) throws Exception {
        Object bean = doGetBean(name, requiredType, null);
        return requiredType != null ? requiredType.cast(bean) : (T) bean;
    }
    
    @Override
    public <T> T getBean(Class<T> requiredType) throws Exception {
        String[] beanNames = getBeanNamesForType(requiredType);
        if (beanNames.length == 0) {
            throw new NoSuchBeanDefinitionException(requiredType);
        }
        if (beanNames.length == 1) {
            return getBean(beanNames[0], requiredType);
        }
        return getPrimaryBean(requiredType);
    }
    
    @Override
    public boolean containsBean(String name) {
        return this.singletonObjects.containsKey(name) || this.beanDefinitionMap.containsKey(name);
    }
    
    @Override
    public boolean isSingleton(String name) {
        BeanDefinition bd = this.beanDefinitionMap.get(name);
        return bd != null && bd.isSingleton();
    }
    
    @Override
    public boolean isPrototype(String name) {
        BeanDefinition bd = this.beanDefinitionMap.get(name);
        return bd != null && bd.isPrototype();
    }
    
    @Override
    public Class<?> getType(String name) {
        BeanDefinition bd = this.beanDefinitionMap.get(name);
        return bd != null ? bd.getBeanClass() : null;
    }
    
    @Override
    public String[] getAliases(String name) {
        return new String[0];
    }
    
    @Override
    public boolean containsBeanDefinition(String beanName) {
        return this.beanDefinitionMap.containsKey(beanName);
    }
    
    @Override
    public int getBeanDefinitionCount() {
        return this.beanDefinitionMap.size();
    }
    
    @Override
    public String[] getBeanDefinitionNames() {
        return this.beanDefinitionMap.keySet().toArray(new String[0]);
    }
    
    @Override
    public String[] getBeanNamesForType(Class<?> type) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, BeanDefinition> entry : this.beanDefinitionMap.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())) {
                result.add(entry.getKey());
            }
        }
        return result.toArray(new String[0]);
    }
    
    @Override
    public <T> List<T> getBeansOfType(Class<T> type) throws Exception {
        List<T> result = new ArrayList<>();
        String[] beanNames = getBeanNamesForType(type);
        for (String beanName : beanNames) {
            result.add(getBean(beanName, type));
        }
        return result;
    }
    
    @Override
    public <T> T getPrimaryBean(Class<T> type) throws Exception {
        String[] beanNames = getBeanNamesForType(type);
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd != null && bd.isPrimary()) {
                return getBean(beanName, type);
            }
        }
        throw new NoSuchBeanDefinitionException("No primary bean found for type: " + type.getName());
    }
    
    protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args) throws Exception {
        BeanDefinition bd = this.beanDefinitionMap.get(name);
        if (bd == null) {
            throw new NoSuchBeanDefinitionException(name);
        }
        
        if (bd.isSingleton()) {
            Object sharedInstance = getSingleton(name);
            if (sharedInstance == null) {
                sharedInstance = createSingletonBean(name, bd);
            }
            return requiredType != null ? requiredType.cast(sharedInstance) : (T) sharedInstance;
        } else {
            Object beanInstance = createBean(name, bd);
            return requiredType != null ? requiredType.cast(beanInstance) : (T) beanInstance;
        }
    }
    
    protected Object getSingleton(String beanName) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null) {
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    singletonObject = singletonFactory.getObject();
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                    logger.debug("Exposed early singleton bean: {}", beanName);
                }
            }
        }
        return singletonObject;
    }
    
    protected Object createSingletonBean(String beanName, BeanDefinition bd) throws Exception {
        Object bean = doCreateBean(beanName, bd);
        addSingleton(beanName, bean);
        return bean;
    }
    
    protected Object createBean(String beanName, BeanDefinition bd) throws Exception {
        return doCreateBean(beanName, bd);
    }
    
    protected Object doCreateBean(String beanName, BeanDefinition bd) throws Exception {
        if (isSingletonCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }
        
        beforeSingletonCreation(beanName);
        
        try {
            Object beanInstance = instantiateBean(beanName, bd);
            
            boolean earlySingletonExposure = bd.isSingleton() && this.singletonsCurrentlyInCreation.contains(beanName);
            if (earlySingletonExposure) {
                addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, bd, beanInstance));
                logger.debug("Exposed early singleton reference for bean: {}", beanName);
            }
            
            populateBean(beanName, bd, beanInstance);
            
            Object exposedObject = initializeBean(beanName, beanInstance, bd);
            
            if (earlySingletonExposure) {
                Object earlySingletonReference = getSingleton(beanName);
                if (earlySingletonReference != null && earlySingletonReference != beanInstance) {
                    exposedObject = earlySingletonReference;
                }
            }
            
            return exposedObject;
        } finally {
            afterSingletonCreation(beanName);
        }
    }
    
    protected Object instantiateBean(String beanName, BeanDefinition bd) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        try {
            Object instance = beanClass.getDeclaredConstructor().newInstance();
            logger.debug("Instantiated bean: {}", beanName);
            return instance;
        } catch (Exception e) {
            throw new BeanCreationException(beanName, "Failed to instantiate bean", e);
        }
    }
    
    protected void populateBean(String beanName, BeanDefinition bd, Object bean) throws Exception {
        Class<?> beanClass = bd.getBeanClass();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            Autowired autowired = field.getAnnotation(Autowired.class);
            if (autowired != null) {
                Object dependency = resolveDependency(field.getType(), field.getName(), autowired.required());
                if (dependency != null) {
                    field.setAccessible(true);
                    field.set(bean, dependency);
                    logger.debug("Autowired field {} in bean {}", field.getName(), beanName);
                } else if (autowired.required()) {
                    throw new BeanCreationException(beanName, 
                        "Required dependency not found for field: " + field.getName());
                }
            }
        }
    }
    
    protected Object initializeBean(String beanName, Object bean, BeanDefinition bd) throws Exception {
        Object wrappedBean = bean;
        
        wrappedBean = applyBeanPostProcessorsBeforeInitialization(wrappedBean, beanName);
        
        invokeInitMethods(beanName, wrappedBean, bd);
        
        wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
        
        return wrappedBean;
    }
    
    protected Object resolveDependency(Class<?> type, String fieldName, boolean required) throws Exception {
        try {
            return getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            if (!required) {
                logger.debug("Optional dependency not found: {}#{}", type.getSimpleName(), fieldName);
                return null;
            }
            throw e;
        }
    }
    
    protected Object getEarlyBeanReference(String beanName, BeanDefinition bd, Object bean) {
        Object exposedObject = bean;
        for (BeanPostProcessor bp : this.beanPostProcessors) {
            exposedObject = bp.getEarlyBeanReference(exposedObject, beanName);
        }
        return exposedObject;
    }
    
    protected void invokeInitMethods(String beanName, Object bean, BeanDefinition bd) throws Exception {
    }
    
    protected Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName) throws Exception {
        Object result = existingBean;
        for (BeanPostProcessor bp : this.beanPostProcessors) {
            Object current = bp.postProcessBeforeInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }
    
    protected Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName) throws Exception {
        Object result = existingBean;
        for (BeanPostProcessor bp : this.beanPostProcessors) {
            Object current = bp.postProcessAfterInitialization(result, beanName);
            if (current == null) {
                return result;
            }
            result = current;
        }
        return result;
    }
    
    protected void addSingleton(String beanName, Object singletonObject) {
        this.singletonObjects.put(beanName, singletonObject);
        this.singletonFactories.remove(beanName);
        this.earlySingletonObjects.remove(beanName);
        this.createdBeanNames.add(beanName);
        logger.debug("Added singleton bean to cache: {}", beanName);
    }
    
    protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
        this.singletonFactories.put(beanName, singletonFactory);
        this.earlySingletonObjects.remove(beanName);
        logger.debug("Added singleton factory for bean: {}", beanName);
    }
    
    protected void beforeSingletonCreation(String beanName) {
        if (!this.singletonsCurrentlyInCreation.add(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }
    }
    
    protected void afterSingletonCreation(String beanName) {
        this.singletonsCurrentlyInCreation.remove(beanName);
    }
    
    protected boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }
    
    public void preInstantiateSingletons() throws Exception {
        List<String> beanNames = new ArrayList<>(this.beanDefinitionMap.keySet());
        
        for (String beanName : beanNames) {
            BeanDefinition bd = this.beanDefinitionMap.get(beanName);
            if (bd != null && bd.isSingleton() && !bd.isLazyInit()) {
                getBean(beanName);
            }
        }
        
        logger.info("Pre-instantiated {} singleton beans", this.singletonObjects.size());
    }
    
    public BeanDefinition getBeanDefinition(String beanName) {
        return this.beanDefinitionMap.get(beanName);
    }
    
    public List<BeanPostProcessor> getBeanPostProcessors() {
        return this.beanPostProcessors;
    }
    
    @FunctionalInterface
    public interface ObjectFactory<T> {
        T getObject() throws Exception;
    }
}