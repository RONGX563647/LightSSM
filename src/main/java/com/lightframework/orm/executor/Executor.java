package com.lightframework.orm.executor;

import com.lightframework.orm.mapping.BoundSql;
import com.lightframework.orm.mapping.MappedStatement;
import com.lightframework.orm.session.ResultHandler;
import com.lightframework.orm.session.RowBounds;
import com.lightframework.orm.transaction.Transaction;

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
