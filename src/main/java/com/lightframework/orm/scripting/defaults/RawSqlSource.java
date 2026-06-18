package com.lightframework.orm.scripting.defaults;

import com.lightframework.orm.builder.SqlSourceBuilder;
import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.mapping.SqlSource;
import com.lightframework.orm.scripting.xmltags.DynamicContext;
import com.lightframework.orm.scripting.xmltags.SqlNode;
import com.lightframework.orm.session.Configuration;

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
