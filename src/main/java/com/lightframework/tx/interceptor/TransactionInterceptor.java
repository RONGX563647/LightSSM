package com.lightframework.tx.interceptor;

import com.lightframework.aop.core.MethodInterceptor;
import com.lightframework.aop.core.MethodInvocation;
import com.lightframework.tx.core.PlatformTransactionManager;
import com.lightframework.tx.core.PlatformTransactionManager.SuspendResources;
import com.lightframework.tx.core.TransactionAttribute;
import com.lightframework.tx.core.TransactionAttributeSource;
import com.lightframework.tx.core.TransactionMetrics;
import com.lightframework.tx.core.TransactionScope;
import com.lightframework.tx.core.TransactionStatus;
import com.lightframework.tx.core.IllegalTransactionStateException;
import com.lightframework.tx.annotation.Propagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.Collections;
import java.util.Map;

public class TransactionInterceptor implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionInterceptor.class);

    private final PlatformTransactionManager defaultTm;
    private final Map<String, PlatformTransactionManager> transactionManagers;
    private final TransactionAttributeSource attributeSource;

    public TransactionInterceptor(PlatformTransactionManager defaultTm,
                                   TransactionAttributeSource attributeSource) {
        this(defaultTm, Collections.emptyMap(), attributeSource);
    }

    public TransactionInterceptor(PlatformTransactionManager defaultTm,
                                   Map<String, PlatformTransactionManager> transactionManagers,
                                   TransactionAttributeSource attributeSource) {
        this.defaultTm = defaultTm;
        this.transactionManagers = transactionManagers;
        this.attributeSource = attributeSource;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        TransactionAttribute attr = attributeSource.getTransactionAttribute(
            invocation.getMethod(), invocation.getTarget().getClass());
        if (attr == null) {
            Propagation scopeOverride = TransactionScope.getOverride();
            if (scopeOverride != null) {
                attr = new TransactionAttribute(scopeOverride);
            } else {
                return invocation.proceed();
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Applying @Transactional({}) on {}.{}",
                attr.getPropagation(),
                invocation.getTarget().getClass().getSimpleName(),
                invocation.getMethod().getName());
        }
        return runWithTransaction(attr, invocation);
    }

    private PlatformTransactionManager resolveTm(TransactionAttribute attr) {
        return attr.resolveManager(defaultTm, transactionManagers);
    }

    private Object runWithTransaction(TransactionAttribute attr, MethodInvocation invocation) throws Throwable {
        PlatformTransactionManager tm = resolveTm(attr);
        Propagation scopeOverride = TransactionScope.getOverride();
        Propagation propagation = scopeOverride != null ? scopeOverride : attr.getPropagation();
        TransactionAttribute effectiveAttr = scopeOverride != null
            ? new TransactionAttribute(attr.getTransactionManagerName(), scopeOverride,
                attr.getIsolation(), attr.getTimeout(), attr.isReadOnly(),
                attr.getRollbackFor(), attr.getNoRollbackFor(), attr.getRetryFor(),
                attr.getMaxRetries(), attr.getRetryDelayMs())
            : attr;
        switch (propagation) {
            case REQUIRED:
                if (tm.hasActiveTransaction()) {
                    return invocation.proceed();
                }
                return runInNewTransaction(tm, attr, invocation);

            case REQUIRES_NEW:
                SuspendResources suspended = tm.suspend();
                try {
                    return runInNewTransaction(tm, attr, invocation);
                } finally {
                    tm.resume(suspended);
                }

            case SUPPORTS:
                return invocation.proceed();

            case MANDATORY:
                if (!tm.hasActiveTransaction()) {
                    throw new IllegalTransactionStateException(
                        "No existing transaction found for " + invocation.getMethod());
                }
                return invocation.proceed();

            case NESTED:
                if (tm.hasActiveTransaction()) {
                    return runWithSavepoint(tm, attr, invocation);
                }
                return runInNewTransaction(tm, attr, invocation);

            case NOT_SUPPORTED:
                SuspendResources suspendedNotSupported = tm.suspend();
                try {
                    return invocation.proceed();
                } finally {
                    tm.resume(suspendedNotSupported);
                }

            case NEVER:
                if (tm.hasActiveTransaction()) {
                    throw new IllegalTransactionStateException(
                        "Existing transaction found for propagation NEVER on " + invocation.getMethod());
                }
                return invocation.proceed();

            default:
                return invocation.proceed();
        }
    }

    private Object runWithSavepoint(PlatformTransactionManager tm, TransactionAttribute attr,
                                     MethodInvocation invocation) throws Throwable {
        Connection conn = tm.getCurrentConnection();
        Savepoint savepoint = conn.setSavepoint();
        try {
            Object result = invocation.proceed();
            conn.releaseSavepoint(savepoint);
            return result;
        } catch (Throwable ex) {
            if (attr.shouldRollbackOn(ex)) {
                conn.rollback(savepoint);
            } else {
                conn.releaseSavepoint(savepoint);
            }
            throw ex;
        }
    }

    private Object runInNewTransaction(PlatformTransactionManager tm, TransactionAttribute attr,
                                        MethodInvocation invocation) throws Throwable {
        int maxRetries = attr.getMaxRetries();
        Throwable lastEx = null;
        for (int i = 0; i <= maxRetries; i++) {
            TransactionStatus status = tm.getTransaction(attr);
            try {
                Object result = invocation.proceed();
                tm.commit(status);
                if (logger.isDebugEnabled()) {
                    logger.debug("Transaction committed for {}.{}",
                        invocation.getTarget().getClass().getSimpleName(),
                        invocation.getMethod().getName());
                }
                return result;
            } catch (Throwable ex) {
                if (maxRetries > 0 && attr.shouldRetryOn(ex) && i < maxRetries) {
                    try {
                        tm.rollback(status);
                    } catch (Exception rollbackEx) {
                        logger.warn("Rollback on retry failed", rollbackEx);
                    }
                    lastEx = ex;
                    if (attr.getRetryDelayMs() > 0) {
                        try { Thread.sleep(attr.getRetryDelayMs()); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    }
                    continue;
                }
                if (attr.shouldRollbackOn(ex)) {
                    TransactionMetrics.recordRollback(ex);
                    try {
                        tm.rollback(status);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Transaction rolled back due to: {}", ex.getMessage());
                        }
                    } catch (Exception rollbackEx) {
                        logger.warn("Rollback failed", rollbackEx);
                    }
                } else {
                    try {
                        tm.commit(status);
                    } catch (Exception commitEx) {
                        logger.warn("Commit after non-rollback exception failed", commitEx);
                    }
                }
                throw ex;
            }
        }
        throw lastEx;
    }

}
