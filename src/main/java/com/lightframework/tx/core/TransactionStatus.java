package com.lightframework.tx.core;

public class TransactionStatus {
    private boolean newTransaction;
    private boolean readOnly;
    private boolean completed;
    private boolean rollbackOnly;

    public TransactionStatus() {}

    TransactionStatus reset(boolean newTransaction, boolean readOnly) {
        this.newTransaction = newTransaction;
        this.readOnly = readOnly;
        this.completed = false;
        this.rollbackOnly = false;
        return this;
    }

    public boolean isNewTransaction() {
        return newTransaction;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    public void setRollbackOnly(boolean rollbackOnly) {
        this.rollbackOnly = rollbackOnly;
    }
}
