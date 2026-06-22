package com.lightframework.tx.event;

public enum TransactionPhase {
    BEFORE_COMMIT,
    AFTER_COMMIT,
    AFTER_ROLLBACK,
    AFTER_COMPLETION
}
