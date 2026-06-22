package com.lightframework.tx.core;

@FunctionalInterface
public interface TransactionCallback<T> {
    T doInTransaction(TransactionStatus status) throws Throwable;
}
