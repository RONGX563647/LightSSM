package com.lightframework.tx.core;

import com.lightframework.tx.annotation.Isolation;
import com.lightframework.tx.annotation.Propagation;
import com.lightframework.tx.annotation.Transactional;

import java.util.Map;

public class TransactionAttribute {
    private final String transactionManagerName;
    private final Propagation propagation;
    private final Isolation isolation;
    private final int timeout;
    private final boolean readOnly;
    private final Class<? extends Throwable>[] rollbackFor;
    private final Class<? extends Throwable>[] noRollbackFor;
    private final Class<? extends Throwable>[] retryFor;
    private final int maxRetries;
    private final int retryDelayMs;

    public TransactionAttribute() {
        this("", Propagation.REQUIRED, Isolation.DEFAULT, -1, false, new Class[0], new Class[0], new Class[0], 0, 100);
    }

    public TransactionAttribute(Propagation propagation) {
        this("", propagation, Isolation.DEFAULT, -1, false, new Class[0], new Class[0], new Class[0], 0, 100);
    }

    @SuppressWarnings("unchecked")
    public TransactionAttribute(Transactional tx) {
        this(tx.value(), tx.propagation(), tx.isolation(), tx.timeout(), tx.readOnly(),
             tx.rollbackFor(), tx.noRollbackFor(), tx.retryFor(), tx.maxRetries(), tx.retryDelayMs());
    }

    public TransactionAttribute(String transactionManagerName, Propagation propagation,
                                Isolation isolation, int timeout, boolean readOnly,
                                Class<? extends Throwable>[] rollbackFor,
                                Class<? extends Throwable>[] noRollbackFor) {
        this(transactionManagerName, propagation, isolation, timeout, readOnly, rollbackFor, noRollbackFor, new Class[0], 0, 100);
    }

    public TransactionAttribute(String transactionManagerName, Propagation propagation,
                                Isolation isolation, int timeout, boolean readOnly,
                                Class<? extends Throwable>[] rollbackFor,
                                Class<? extends Throwable>[] noRollbackFor,
                                Class<? extends Throwable>[] retryFor,
                                int maxRetries, int retryDelayMs) {
        this.transactionManagerName = transactionManagerName;
        this.propagation = propagation;
        this.isolation = isolation;
        this.timeout = timeout;
        this.readOnly = readOnly;
        this.rollbackFor = rollbackFor != null ? rollbackFor : new Class[0];
        this.noRollbackFor = noRollbackFor != null ? noRollbackFor : new Class[0];
        this.retryFor = retryFor != null ? retryFor : new Class[0];
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    public String getTransactionManagerName() { return transactionManagerName; }
    public Propagation getPropagation() { return propagation; }
    public Isolation getIsolation() { return isolation; }
    public int getTimeout() { return timeout; }
    public boolean isReadOnly() { return readOnly; }
    public Class<? extends Throwable>[] getRollbackFor() { return rollbackFor; }
    public Class<? extends Throwable>[] getNoRollbackFor() { return noRollbackFor; }
    public Class<? extends Throwable>[] getRetryFor() { return retryFor; }
    public int getMaxRetries() { return maxRetries; }
    public int getRetryDelayMs() { return retryDelayMs; }
    private volatile PlatformTransactionManager resolvedManager;
    public PlatformTransactionManager resolveManager(PlatformTransactionManager defaultTm,
                                                      Map<String, PlatformTransactionManager> managers) {
        if (resolvedManager != null) return resolvedManager;
        if (transactionManagerName.isEmpty()) {
            resolvedManager = defaultTm;
        } else {
            resolvedManager = managers.get(transactionManagerName);
            if (resolvedManager == null) {
                throw new IllegalTransactionStateException(
                    "No transaction manager found with name '" + transactionManagerName + "'");
            }
        }
        return resolvedManager;
    }

    public boolean shouldRetryOn(Throwable ex) {
        if (retryFor.length == 0) return maxRetries > 0;
        for (Class<? extends Throwable> clazz : retryFor) {
            if (clazz.isInstance(ex)) return true;
        }
        return false;
    }

    public boolean shouldRollbackOn(Throwable ex) {
        Class<?> bestMatch = null;
        boolean fromRollbackFor = true;
        int bestDepth = Integer.MAX_VALUE;

        for (Class<? extends Throwable> clazz : rollbackFor) {
            if (clazz.isInstance(ex)) {
                int depth = depthInHierarchy(clazz, ex.getClass());
                if (depth < bestDepth) {
                    bestDepth = depth;
                    fromRollbackFor = true;
                    bestMatch = clazz;
                }
            }
        }
        for (Class<? extends Throwable> clazz : noRollbackFor) {
            if (clazz.isInstance(ex)) {
                int depth = depthInHierarchy(clazz, ex.getClass());
                if (depth < bestDepth) {
                    bestDepth = depth;
                    fromRollbackFor = false;
                    bestMatch = clazz;
                }
            }
        }

        if (bestMatch != null) {
            return fromRollbackFor;
        }
        return ex instanceof RuntimeException || ex instanceof Error;
    }

    private static int depthInHierarchy(Class<?> ancestor, Class<?> descendant) {
        int depth = 0;
        Class<?> current = descendant;
        while (current != ancestor && current != null) {
            current = current.getSuperclass();
            depth++;
        }
        return current == ancestor ? depth : -1;
    }
}
