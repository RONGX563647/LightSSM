package com.lightframework.tx.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionTemplate {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTemplate.class);

    private final PlatformTransactionManager transactionManager;

    public TransactionTemplate(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public <T> T execute(TransactionCallback<T> action) throws Throwable {
        return execute(new TransactionAttribute(), action);
    }

    public <T> T execute(TransactionAttribute attr, TransactionCallback<T> action) throws Throwable {
        TransactionStatus status = transactionManager.getTransaction(attr);
        try {
            T result = action.doInTransaction(status);
            transactionManager.commit(status);
            if (logger.isDebugEnabled()) {
                logger.debug("TransactionTemplate commit");
            }
            return result;
        } catch (Throwable ex) {
            if (attr.shouldRollbackOn(ex)) {
                try {
                    transactionManager.rollback(status);
                    if (logger.isDebugEnabled()) {
                        logger.debug("TransactionTemplate rollback due to: {}", ex.getMessage());
                    }
                } catch (Exception rollbackEx) {
                    logger.warn("TransactionTemplate rollback failed", rollbackEx);
                }
            } else {
                try {
                    transactionManager.commit(status);
                } catch (Exception commitEx) {
                    logger.warn("TransactionTemplate commit after non-rollback exception failed", commitEx);
                }
            }
            throw ex;
        }
    }
}
