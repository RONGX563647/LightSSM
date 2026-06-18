package com.lightframework.ioc.context;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;

public class LightActuator {

    @Autowired
    private DefaultListableBeanFactory beanFactory;

    public LightActuator() {}

    public LightActuator(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public void setBeanFactory(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public String report() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LightSSM IoC Container Report ===\n");
        sb.append("Beans: ").append(beanFactory.getBeanDefinitionCount()).append("\n");
        sb.append("Singletons: ").append(beanFactory.getSingletonCount()).append("\n");
        sb.append("Startup: ").append(beanFactory.getStartupDuration()).append("ms\n\n");
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            sb.append(String.format("  %-30s %s %s\n",
                name, bd.isSingleton() ? "S" : "P", bd.getBeanClass().getSimpleName()));
        }
        sb.append("\n--- Dependency Graph ---\n");
        beanFactory.dumpGraph(sb);
        return sb.toString();
    }
}
