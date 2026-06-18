package com.lightframework.orm.scripting;

import com.lightframework.orm.executor.parameter.ParameterHandler;
import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.mapping.MappedStatement;
import com.lightframework.orm.mapping.SqlSource;
import com.lightframework.orm.session.Configuration;
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
