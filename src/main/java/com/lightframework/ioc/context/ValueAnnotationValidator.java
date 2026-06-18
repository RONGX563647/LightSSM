package com.lightframework.ioc.context;

import com.lightframework.di.annotation.Value;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.PropertyPlaceholderConfigurer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ValueAnnotationValidator {

    public void validate(DefaultListableBeanFactory beanFactory) throws Exception {
        PropertyPlaceholderConfigurer configurer = beanFactory.getPropertyPlaceholderConfigurer();
        if (configurer == null) return;

        List<String> unresolved = new ArrayList<>();
        for (String name : beanFactory.getBeanDefinitionNames()) {
            Class<?> beanClass = beanFactory.getBeanDefinition(name).getBeanClass();
            if (beanClass == null) continue;
            for (Field f : beanClass.getDeclaredFields()) {
                Value v = f.getAnnotation(Value.class);
                if (v != null && v.value().contains("${")) {
                    String placeholder = v.value();
                    int start = placeholder.indexOf("${") + 2;
                    int end = placeholder.indexOf(":", start);
                    if (end == -1) end = placeholder.indexOf("}", start);
                    String key = placeholder.substring(start, end).trim();
                    if (configurer.getProperty(key) == null) {
                        unresolved.add(name + "." + f.getName() + " (" + v.value() + ")");
                    }
                }
            }
        }
        if (!unresolved.isEmpty()) {
            System.err.println("WARN: Unresolved @Value placeholders:\n  - " +
                String.join("\n  - ", unresolved));
        }
    }
}
