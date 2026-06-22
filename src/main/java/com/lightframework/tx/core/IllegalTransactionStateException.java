package com.lightframework.tx.core;

public class IllegalTransactionStateException extends RuntimeException {
    public IllegalTransactionStateException(String message) {
        super(message);
    }
}
