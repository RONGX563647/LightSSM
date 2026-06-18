package com.lightframework.spi.mybatis.autoconfigure;

import com.lightframework.di.annotation.Component;
import com.lightframework.ioc.core.DefaultListableBeanFactory;
import com.lightframework.ioc.core.ImportBeanDefinitionRegistrar;
import com.lightframework.spi.mybatis.MapperScannerConfigurer;
import com.lightframework.spi.mybatis.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class MapperScanRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(MapperScanRegistrar.class);

    @Override
    public void registerBeanDefinitions(DefaultListableBeanFactory registry) {
        String[] beanNames = registry.getBeanDefinitionNames();
        String basePackage = null;
        for (String beanName : beanNames) {
            com.lightframework.ioc.beans.BeanDefinition bd = registry.getBeanDefinition(beanName);
            if (bd != null && bd.getBeanClass() != null) {
                MapperScan scan = bd.getBeanClass().getAnnotation(MapperScan.class);
                if (scan != null) {
                    String[] packages = scan.value();
                    if (packages.length > 0) {
                        basePackage = packages[0];
                    } else if (!scan.basePackage().isEmpty()) {
                        basePackage = scan.basePackage();
                    }
                    if (basePackage != null && !basePackage.isEmpty()) {
                        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
                        scanner.setBasePackage(basePackage);
                        try {
                            scanner.postProcessBeanDefinitionRegistry(registry);
                        } catch (Exception e) {
                            logger.warn("Failed to scan mappers for package: {}", basePackage, e);
                        }
                    }
                    break;
                }
            }
        }
    }
}
