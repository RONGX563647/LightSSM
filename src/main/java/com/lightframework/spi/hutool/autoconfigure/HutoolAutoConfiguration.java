package com.lightframework.spi.hutool.autoconfigure;

import com.lightframework.ioc.annotation.Bean;
import com.lightframework.ioc.annotation.Configuration;
import com.lightframework.spi.annotation.ConditionalOnClass;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import com.lightframework.spi.cache.CacheManager;
import com.lightframework.spi.hutool.HutoolBeanMapper;
import com.lightframework.spi.hutool.HutoolCacheManager;
import com.lightframework.spi.hutool.HutoolJsonSerializer;
import com.lightframework.spi.hutool.HutoolValidator;
import com.lightframework.spi.json.JsonSerializer;
import com.lightframework.spi.mapper.BeanMapper;
import com.lightframework.spi.validate.Validator;

@Configuration
@ConditionalOnClass("cn.hutool.core.bean.BeanUtil")
public class HutoolAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BeanMapper.class)
    public BeanMapper beanMapper() {
        return new HutoolBeanMapper();
    }

    @Bean
    @ConditionalOnMissingBean(JsonSerializer.class)
    public JsonSerializer jsonSerializer() {
        return new HutoolJsonSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager() {
        return new HutoolCacheManager();
    }

    @Bean
    @ConditionalOnMissingBean(Validator.class)
    public Validator validator() {
        return new HutoolValidator();
    }
}
