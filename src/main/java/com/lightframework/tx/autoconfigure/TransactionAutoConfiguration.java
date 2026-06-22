package com.lightframework.tx.autoconfigure;

import com.lightframework.di.annotation.Bean;
import com.lightframework.di.annotation.Configuration;
import com.lightframework.spi.annotation.ConditionalOnClass;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import com.lightframework.tx.core.DataSourceTransactionManager;
import com.lightframework.tx.core.PlatformTransactionManager;
import com.lightframework.tx.interceptor.TransactionalBeanPostProcessor;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass("javax.sql.DataSource")
public class TransactionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(TransactionalBeanPostProcessor.class)
    public TransactionalBeanPostProcessor txBeanPostProcessor(PlatformTransactionManager transactionManager) {
        return new TransactionalBeanPostProcessor(transactionManager);
    }
}
