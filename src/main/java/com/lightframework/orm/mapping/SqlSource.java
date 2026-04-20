package com.rongx.mybatis.mapping;


public interface SqlSource {

    BoundSql getBoundSql(Object parameterObject);

}
