package com.lightframework.spi.jackson.autoconfigure;

import com.lightframework.di.annotation.Bean;
import com.lightframework.di.annotation.Configuration;
import com.lightframework.spi.annotation.ConditionalOnClass;
import com.lightframework.spi.annotation.ConditionalOnMissingBean;
import com.lightframework.spi.jackson.JacksonSerializer;
import com.lightframework.spi.json.JsonSerializer;

@Configuration
@ConditionalOnClass("com.fasterxml.jackson.databind.ObjectMapper")
public class JacksonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JsonSerializer.class)
    public JsonSerializer jsonSerializer() {
        return new JacksonSerializer();
    }
}
