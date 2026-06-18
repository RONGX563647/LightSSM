package com.lightframework.orm.mapping;


public interface SqlSource {

    BoundSql getBoundSql(Object parameterObject);

}
