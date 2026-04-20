package com.rongx.mybatis.scripting;

import com.rongx.mybatis.executor.parameter.ParameterHandler;
import com.rongx.mybatis.mapping.BoundSql;
import com.rongx.mybatis.mapping.MappedStatement;
import com.rongx.mybatis.mapping.SqlSource;
import com.rongx.mybatis.session.Configuration;
import org.dom4j.Element;


public interface LanguageDriver {

    /**
     * 创建SQL源码(mapper xml方式)
     */
    SqlSource createSqlSource(Configuration configuration, Element script, Class<?> parameterType);

    /**
     * 创建SQL源码(annotation 注解方式)
     */
    SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

    /**
     * 创建参数处理器
     */
    ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

}
