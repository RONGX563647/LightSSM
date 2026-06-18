package com.lightframework.orm.session;

import com.lightframework.orm.builder.xml.XMLConfigBuilder;
import com.lightframework.orm.session.defaults.DefaultSqlSessionFactory;

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
