package com.lightframework.spi.mybatis;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.ibatis.cursor.Cursor;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class MyBatisSqlSessionTemplate implements SqlSession {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisSqlSessionTemplate.class);

    private final SqlSessionFactory sqlSessionFactory;
    private final ExecutorType executorType;
    private final ThreadLocal<SqlSession> sessionHolder = new ThreadLocal<>();

    public MyBatisSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = ExecutorType.SIMPLE;
    }

    public MyBatisSqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = executorType;
    }

    public SqlSession getSqlSession() {
        SqlSession session = sessionHolder.get();
        if (session == null) {
            session = sqlSessionFactory.openSession(executorType);
            sessionHolder.set(session);
            if (logger.isTraceEnabled()) {
                logger.trace("Opened new SqlSession for thread: {}", Thread.currentThread().getName());
            }
        }
        return session;
    }

    public void commit() {
        SqlSession session = sessionHolder.get();
        if (session != null) {
            try {
                session.commit();
            } finally {
                session.close();
                sessionHolder.remove();
            }
        }
    }

    public void rollback() {
        SqlSession session = sessionHolder.get();
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
                sessionHolder.remove();
            }
        }
    }

    public <T> T getMapper(Class<T> mapperInterface) {
        return getSqlSession().getMapper(mapperInterface);
    }

    // ===== SqlSession delegation =====

    @Override
    public <T> T selectOne(String statement) {
        return getSqlSession().selectOne(statement);
    }

    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return getSqlSession().selectOne(statement, parameter);
    }

    @Override
    public <E> List<E> selectList(String statement) {
        return getSqlSession().selectList(statement);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return getSqlSession().selectList(statement, parameter);
    }

    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return getSqlSession().selectList(statement, parameter, rowBounds);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return getSqlSession().selectMap(statement, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return getSqlSession().selectMap(statement, parameter, mapKey);
    }

    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        return getSqlSession().selectMap(statement, parameter, mapKey, rowBounds);
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement) {
        return getSqlSession().selectCursor(statement);
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter) {
        return getSqlSession().selectCursor(statement, parameter);
    }

    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
        return getSqlSession().selectCursor(statement, parameter, rowBounds);
    }

    @Override
    public void select(String statement, ResultHandler handler) {
        getSqlSession().select(statement, handler);
    }

    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        getSqlSession().select(statement, parameter, handler);
    }

    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        getSqlSession().select(statement, parameter, rowBounds, handler);
    }

    @Override
    public int insert(String statement) {
        return getSqlSession().insert(statement);
    }

    @Override
    public int insert(String statement, Object parameter) {
        return getSqlSession().insert(statement, parameter);
    }

    @Override
    public int update(String statement) {
        return getSqlSession().update(statement);
    }

    @Override
    public int update(String statement, Object parameter) {
        return getSqlSession().update(statement, parameter);
    }

    @Override
    public int delete(String statement) {
        return getSqlSession().delete(statement);
    }

    @Override
    public int delete(String statement, Object parameter) {
        return getSqlSession().delete(statement, parameter);
    }

    @Override
    public void commit(boolean force) {
        getSqlSession().commit(force);
    }

    @Override
    public void rollback(boolean force) {
        getSqlSession().rollback(force);
    }

    @Override
    public List<BatchResult> flushStatements() {
        return getSqlSession().flushStatements();
    }

    @Override
    public void close() {
        SqlSession session = sessionHolder.get();
        if (session != null) {
            try {
                session.close();
            } finally {
                sessionHolder.remove();
            }
        }
    }

    @Override
    public void clearCache() {
        getSqlSession().clearCache();
    }

    @Override
    public Configuration getConfiguration() {
        return sqlSessionFactory.getConfiguration();
    }

    @Override
    public Connection getConnection() {
        return getSqlSession().getConnection();
    }
}
