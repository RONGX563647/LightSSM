package com.lightframework.spi.caffeine.autoconfigure;

import com.lightframework.ioc.annotation.Bean;
import com.lightframework.ioc.annotation.Configuration;
import com.lightframework.spi.annotation.ConditionalOnClass;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import com.lightframework.spi.cache.CacheManager;
import com.lightframework.spi.caffeine.CaffeineCacheManager;

@Configuration
@ConditionalOnClass("com.github.benmanes.caffeine.cache.Caffeine")
public class CaffeineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new CaffeineCacheManager();
    }
}
