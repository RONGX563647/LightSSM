package com.rongx.mybatis.session;

import com.rongx.mybatis.builder.xml.XMLConfigBuilder;
import com.rongx.mybatis.session.defaults.DefaultSqlSessionFactory;

import java.io.Reader;


public class SqlSessionFactoryBuilder {

    public SqlSessionFactory build(Reader reader) {
        XMLConfigBuilder xmlConfigBuilder = new XMLConfigBuilder(reader);
        return build(xmlConfigBuilder.parse());
    }

    public SqlSessionFactory build(Configuration config) {
        return new DefaultSqlSessionFactory(config);
    }

}
