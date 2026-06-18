package com.lightframework.spi.mybatis;

import com.lightframework.ioc.annotation.Autowired;
import com.lightframework.ioc.core.FactoryBean;
import com.lightframework.ioc.core.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyBatisMapperFactoryBean<T> implements FactoryBean<T>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisMapperFactoryBean.class);

    private Class<T> mapperInterface;

    @Autowired
    private MyBatisSqlSessionTemplate sqlSessionTemplate;

    private T cachedMapper;

    @Override
    public void afterPropertiesSet() {
        if (mapperInterface == null) {
            throw new IllegalArgumentException("mapperInterface is required");
        }
        if (sqlSessionTemplate == null) {
            throw new IllegalArgumentException("sqlSessionTemplate is required");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Mapper factory initialized for: {}", mapperInterface.getName());
        }
    }

    @Override
    public T getObject() {
        if (cachedMapper == null) {
            cachedMapper = sqlSessionTemplate.getMapper(mapperInterface);
        }
        return cachedMapper;
    }

    @Override
    public Class<?> getObjectType() {
        return mapperInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }

    public void setSqlSessionTemplate(MyBatisSqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
}
