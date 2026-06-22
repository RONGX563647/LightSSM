package com.lightframework.tx.core;

public interface TransactionSynchronization {

    int STATUS_COMMITTED = 0;
    int STATUS_ROLLED_BACK = 1;
    int STATUS_UNKNOWN = 2;

    default void beforeCommit(boolean readOnly) throws Exception {}

    default void afterCompletion(int status) {}
}
