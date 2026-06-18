package com.lightframework.orm.session.defaults;

import com.lightframework.orm.executor.Executor;
import com.lightframework.orm.mapping.Environment;
import com.lightframework.orm.session.Configuration;
import com.lightframework.orm.session.SqlSession;
import com.lightframework.orm.session.SqlSessionFactory;
import com.lightframework.orm.session.TransactionIsolationLevel;
import com.lightframework.orm.transaction.Transaction;
import com.lightframework.orm.transaction.TransactionFactory;

import java.sql.SQLException;


public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SqlSession openSession() {
        Transaction tx = null;
        try {
            final Environment environment = configuration.getEnvironment();
            TransactionFactory transactionFactory = environment.getTransactionFactory();
            tx = transactionFactory.newTransaction(configuration.getEnvironment().getDataSource(), TransactionIsolationLevel.READ_COMMITTED, false);
            // 创建执行器
            final Executor executor = configuration.newExecutor(tx);
            // 创建DefaultSqlSession
            return new DefaultSqlSession(configuration, executor);
        } catch (Exception e) {
            try {
                assert tx != null;
                tx.close();
            } catch (SQLException ignore) {
            }
            throw new RuntimeException("Error opening session.  Cause: " + e);
        }
    }

}
