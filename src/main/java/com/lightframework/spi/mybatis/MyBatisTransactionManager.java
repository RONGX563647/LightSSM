package com.lightframework.spi.mybatis;

import com.lightframework.tx.core.DataSourceTransactionManager;
import com.lightframework.tx.core.TransactionStatus;
import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

public class MyBatisTransactionManager extends DataSourceTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisTransactionManager.class);

    private MyBatisSqlSessionTemplate sqlSessionTemplate;

    public MyBatisTransactionManager(DataSource dataSource) {
        super(dataSource);
    }

    public void setSqlSessionTemplate(MyBatisSqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @Override
    public void commit(TransactionStatus status) throws Exception {
        flushSqlSessionIfNeeded();
        super.commit(status);
        closeSqlSession();
    }

    @Override
    public void rollback(TransactionStatus status) throws Exception {
        super.rollback(status);
        closeSqlSession();
    }

    private void flushSqlSessionIfNeeded() {
        if (sqlSessionTemplate != null) {
            SqlSession session = sqlSessionTemplate.getCurrentSession();
            if (session != null) {
                try {
                    session.flushStatements();
                } catch (Exception e) {
                    logger.warn("Failed to flush SqlSession statements", e);
                }
            }
        }
    }

    private void closeSqlSession() {
        if (sqlSessionTemplate != null) {
            sqlSessionTemplate.close();
        }
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction() throws Exception;
    }
}
