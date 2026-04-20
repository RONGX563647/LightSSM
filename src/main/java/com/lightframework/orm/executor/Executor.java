package com.rongx.mybatis.executor;

import com.rongx.mybatis.mapping.BoundSql;
import com.rongx.mybatis.mapping.MappedStatement;
import com.rongx.mybatis.session.ResultHandler;
import com.rongx.mybatis.session.RowBounds;
import com.rongx.mybatis.transaction.Transaction;

import java.sql.SQLException;
import java.util.List;


public interface Executor {

    ResultHandler NO_RESULT_HANDLER = null;

    int update(MappedStatement ms, Object parameter) throws SQLException;

    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException;

    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;

    Transaction getTransaction();

    void commit(boolean required) throws SQLException;

    void rollback(boolean required) throws SQLException;

    void close(boolean forceRollback);

}
