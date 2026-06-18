package com.lightframework.orm.transaction.jdbc;

import com.lightframework.orm.session.TransactionIsolationLevel;
import com.lightframework.orm.transaction.Transaction;
import com.lightframework.orm.transaction.TransactionFactory;

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
