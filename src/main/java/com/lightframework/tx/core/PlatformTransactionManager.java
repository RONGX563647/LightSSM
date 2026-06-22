package com.lightframework.tx.core;

import java.sql.Connection;

public interface PlatformTransactionManager {

    class SuspendResources {
        public Connection connection;
        public int refCount;
        public boolean wasActive;
        public SuspendResources() {}
        public SuspendResources(Connection connection, int refCount, boolean wasActive) {
            fill(connection, refCount, wasActive);
        }
        public SuspendResources fill(Connection connection, int refCount, boolean wasActive) {
            this.connection = connection;
            this.refCount = refCount;
            this.wasActive = wasActive;
            return this;
        }
    }

    TransactionStatus getTransaction(TransactionAttribute definition) throws Exception;
    void commit(TransactionStatus status) throws Exception;
    void rollback(TransactionStatus status) throws Exception;
    boolean hasActiveTransaction();
    Connection getCurrentConnection();
    SuspendResources suspend();
    void resume(SuspendResources resources);
}
