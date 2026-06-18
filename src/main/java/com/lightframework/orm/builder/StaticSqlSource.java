package com.lightframework.orm.builder;

import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.mapping.ParameterMapping;
import com.lightframework.orm.mapping.SqlSource;
import com.lightframework.orm.session.Configuration;

import java.util.List;


public class StaticSqlSource implements SqlSource {

    private String sql;
    private List<ParameterMapping> parameterMappings;
    private Configuration configuration;

    public StaticSqlSource(Configuration configuration, String sql) {
        this(configuration, sql, null);
    }

    public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
        this.sql = sql;
        this.parameterMappings = parameterMappings;
        this.configuration = configuration;
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return new BoundSql(configuration, sql, parameterMappings, parameterObject);
    }

}
