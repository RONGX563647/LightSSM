package com.lightframework.spi.mybatis.autoconfigure;

import com.lightframework.di.annotation.*;
import com.lightframework.spi.annotation.ConditionalOnClass;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import com.lightframework.spi.mybatis.MapperScannerConfigurer;
import com.lightframework.spi.mybatis.MyBatisSqlSessionTemplate;
import com.lightframework.spi.mybatis.MyBatisTransactionManager;
import com.lightframework.spi.mybatis.SqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass("org.apache.ibatis.session.SqlSessionFactory")
public class MyBatisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SqlSessionFactoryBean.class)
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource, MyBatisProperties properties) {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperBasePackage(properties.getMapperBasePackage());
        if (properties.getConfigLocation() != null) {
            factory.setConfigLocation(properties.getConfigLocation());
        }
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(MyBatisTransactionManager.class)
    public MyBatisTransactionManager transactionManager(DataSource dataSource) {
        return new MyBatisTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(MyBatisSqlSessionTemplate.class)
    public MyBatisSqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory,
                                                          MyBatisTransactionManager transactionManager) {
        MyBatisSqlSessionTemplate template = new MyBatisSqlSessionTemplate(sqlSessionFactory);
        template.setTransactionManager(transactionManager);
        transactionManager.setSqlSessionTemplate(template);
        return template;
    }

    @Bean
    @ConditionalOnMissingBean(MyBatisProperties.class)
    public MyBatisProperties myBatisProperties() {
        return new MyBatisProperties();
    }

    @Bean
    @ConditionalOnMissingBean(MapperScannerConfigurer.class)
    public MapperScannerConfigurer mapperScannerConfigurer(MyBatisProperties properties) {
        MapperScannerConfigurer scanner = new MapperScannerConfigurer();
        scanner.setBasePackage(properties.getMapperBasePackage());
        return scanner;
    }
}
