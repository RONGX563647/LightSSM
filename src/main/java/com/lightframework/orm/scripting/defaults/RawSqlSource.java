package com.rongx.mybatis.scripting.defaults;

import com.rongx.mybatis.builder.SqlSourceBuilder;
import com.rongx.mybatis.mapping.BoundSql;
import com.rongx.mybatis.mapping.SqlSource;
import com.rongx.mybatis.scripting.xmltags.DynamicContext;
import com.rongx.mybatis.scripting.xmltags.SqlNode;
import com.rongx.mybatis.session.Configuration;

import java.util.HashMap;


public class RawSqlSource implements SqlSource {

    private final SqlSource sqlSource;

    public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
        this(configuration, getSql(configuration, rootSqlNode), parameterType);
    }

    public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        Class<?> clazz = parameterType == null ? Object.class : parameterType;
        sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
    }

    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        return sqlSource.getBoundSql(parameterObject);
    }

    private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
        DynamicContext context = new DynamicContext(configuration, null);
        rootSqlNode.apply(context);
        return context.getSql();
    }

}
