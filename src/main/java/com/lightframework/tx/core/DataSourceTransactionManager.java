package com.lightframework.tx.core;

import com.lightframework.tx.annotation.Isolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayDeque;

public class DataSourceTransactionManager implements PlatformTransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceTransactionManager.class);

    private final DataSource dataSource;

    static final class ThreadLocalState {
        final TransactionContext txContext = new TransactionContext();
        final ArrayDeque<TransactionStatus> statusPool = new ArrayDeque<>();
        final ArrayDeque<SuspendResources> suspendPool = new ArrayDeque<>();
    }

    private final ThreadLocal<ThreadLocalState> state = ThreadLocal.withInitial(ThreadLocalState::new);

    public DataSourceTransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    static final class TransactionContext {
        Connection connection;
        boolean active;
        int referenceCount;
        boolean readOnly;

        void clear() {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (Exception e) {
                    logger.warn("Failed to close connection", e);
                }
                connection = null;
            }
            active = false;
            referenceCount = 0;
            readOnly = false;
        }
    }

    private TransactionStatus obtainStatus(boolean newTransaction, boolean readOnly) {
        ThreadLocalState s = state.get();
        TransactionStatus st = s.statusPool.pollFirst();
        if (st == null) st = new TransactionStatus();
        st.reset(newTransaction, readOnly);
        return st;
    }

    private void recycleStatus(TransactionStatus s) {
        state.get().statusPool.addFirst(s);
    }

    private SuspendResources obtainSuspendResources(Connection conn, int refCount, boolean wasActive) {
        ThreadLocalState s = state.get();
        SuspendResources sr = s.suspendPool.pollFirst();
        if (sr == null) sr = new SuspendResources();
        sr.fill(conn, refCount, wasActive);
        return sr;
    }

    private void recycleSuspendResources(SuspendResources sr) {
        state.get().suspendPool.addFirst(sr);
    }

    @Override
    public TransactionStatus getTransaction(TransactionAttribute definition) throws Exception {
        TransactionContext ctx = state.get().txContext;
        if (ctx.active) {
            ctx.referenceCount++;
            if (logger.isTraceEnabled()) {
                logger.trace("Joining existing transaction, referenceCount={}", ctx.referenceCount);
            }
            return obtainStatus(false, ctx.readOnly);
        }
        Connection conn = dataSource.getConnection();
        conn.setAutoCommit(false);
        if (definition.isReadOnly()) {
            conn.setReadOnly(true);
        }
        if (definition.getIsolation() != Isolation.DEFAULT) {
            conn.setTransactionIsolation(mapIsolation(definition.getIsolation()));
        }
        if (definition.getTimeout() > 0) {
            conn.setNetworkTimeout(null, definition.getTimeout() * 1000);
        }
        ctx.connection = conn;
        ctx.active = true;
        ctx.referenceCount = 1;
        ctx.readOnly = definition.isReadOnly();
        TransactionStatus status = obtainStatus(true, definition.isReadOnly());
        TransactionSynchronizationManager.onBegin();
        if (logger.isDebugEnabled()) {
            logger.debug("Began new JDBC transaction, isolation={}", definition.getIsolation());
        }
        return status;
    }

    @Override
    public void commit(TransactionStatus status) throws Exception {
        if (status.isCompleted()) return;
        TransactionContext ctx = state.get().txContext;
        if (!status.isNewTransaction() && ctx.referenceCount > 1) {
            ctx.referenceCount--;
            recycleStatus(status);
            if (logger.isTraceEnabled()) {
                logger.trace("Decreased referenceCount to {}", ctx.referenceCount);
            }
            return;
        }
        if (status.isRollbackOnly()) {
            rollback(status);
            return;
        }
        if (ctx.connection != null) {
            TransactionSynchronizationManager.beforeCommit(ctx.readOnly);
            ctx.connection.commit();
            TransactionMetrics.commitCount.increment();
            if (logger.isDebugEnabled()) {
                logger.debug("JDBC transaction committed");
            }
        }
        TransactionSynchronizationManager.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        TransactionSynchronizationManager.onEnd();
        ctx.clear();
        recycleStatus(status);
    }

    @Override
    public void rollback(TransactionStatus status) throws Exception {
        if (status.isCompleted()) return;
        TransactionContext ctx = state.get().txContext;
        if (!status.isNewTransaction() && ctx.referenceCount > 1) {
            status.setRollbackOnly(true);
            ctx.referenceCount--;
            recycleStatus(status);
            if (logger.isTraceEnabled()) {
                logger.trace("Marked rollback-only, decreased referenceCount to {}", ctx.referenceCount);
            }
            return;
        }
        if (ctx.connection != null) {
            try {
                ctx.connection.rollback();
                TransactionMetrics.rollbackCount.increment();
                if (logger.isDebugEnabled()) {
                    logger.debug("JDBC transaction rolled back");
                }
            } catch (Exception e) {
                logger.warn("Rollback failed", e);
            }
        }
        TransactionSynchronizationManager.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        TransactionSynchronizationManager.onEnd();
        ctx.clear();
        recycleStatus(status);
    }

    @Override
    public boolean hasActiveTransaction() {
        return state.get().txContext.active;
    }

    @Override
    public Connection getCurrentConnection() {
        return state.get().txContext.connection;
    }

    @Override
    public SuspendResources suspend() {
        TransactionContext ctx = state.get().txContext;
        Connection saved = ctx.connection;
        int refCount = ctx.referenceCount;
        boolean wasActive = ctx.active;
        if (saved != null && logger.isTraceEnabled()) {
            logger.trace("Suspending transaction (Connection={}, refCount={})", saved, refCount);
        }
        ctx.connection = null;
        ctx.active = false;
        ctx.referenceCount = 0;
        ctx.readOnly = false;
        return obtainSuspendResources(saved, refCount, wasActive);
    }

    @Override
    public void resume(SuspendResources resources) {
        if (resources == null || resources.connection == null) return;
        TransactionContext ctx = state.get().txContext;
        ctx.connection = resources.connection;
        ctx.active = resources.wasActive;
        ctx.referenceCount = resources.refCount;
        if (logger.isTraceEnabled()) {
            logger.trace("Resumed transaction (Connection={}, refCount={})", resources.connection, resources.refCount);
        }
        recycleSuspendResources(resources);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    protected void cleanup() throws Exception {
        state.get().txContext.clear();
        state.remove();
    }

    private int mapIsolation(Isolation isolation) {
        switch (isolation) {
            case READ_UNCOMMITTED: return Connection.TRANSACTION_READ_UNCOMMITTED;
            case READ_COMMITTED:   return Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ:  return Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE:     return Connection.TRANSACTION_SERIALIZABLE;
            default:               throw new AssertionError("Unexpected isolation: " + isolation);
        }
    }
}
