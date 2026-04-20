package com.rongx.mybatis.builder;

import com.rongx.mybatis.mapping.BoundSql;
import com.rongx.mybatis.mapping.ParameterMapping;
import com.rongx.mybatis.mapping.SqlSource;
import com.rongx.mybatis.session.Configuration;

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
