package com.lightframework.spi.mybatis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;

public class MyBatisTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(MyBatisTransactionManager.class);

    private final DataSource dataSource;
    private final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    private final ThreadLocal<Boolean> active = new ThreadLocal<>();

    public MyBatisTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T executeInTransaction(TransactionCallback<T> callback) throws Exception {
        if (Boolean.TRUE.equals(active.get())) {
            return callback.doInTransaction();
        }
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            connectionHolder.set(conn);
            active.set(true);
            T result = callback.doInTransaction();
            conn.commit();
            if (logger.isDebugEnabled()) {
                logger.debug("Transaction committed successfully");
            }
            return result;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Transaction rolled back due to: {}", e.getMessage());
                    }
                } catch (Exception rollbackEx) {
                    logger.warn("Rollback failed", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception e) {
                    logger.warn("Failed to close connection", e);
                }
            }
            connectionHolder.remove();
            active.remove();
        }
    }

    public Connection getCurrentConnection() {
        return connectionHolder.get();
    }

    public boolean hasActiveTransaction() {
        return Boolean.TRUE.equals(active.get());
    }

    @FunctionalInterface
    public interface TransactionCallback<T> {
        T doInTransaction() throws Exception;
    }
}
