package com.rongx.mybatis.transaction.jdbc;

import com.rongx.mybatis.session.TransactionIsolationLevel;
import com.rongx.mybatis.transaction.Transaction;
import com.rongx.mybatis.transaction.TransactionFactory;

import javax.sql.DataSource;
import java.sql.Connection;


public class JdbcTransactionFactory implements TransactionFactory {

    @Override
    public Transaction newTransaction(Connection conn) {
        return new JdbcTransaction(conn);
    }

    @Override
    public Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit) {
        return new JdbcTransaction(dataSource, level, autoCommit);
    }

}
