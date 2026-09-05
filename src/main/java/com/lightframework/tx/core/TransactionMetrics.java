package com.lightframework.tx.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class TransactionMetrics {

    static final LongAdder commitCount = new LongAdder();
    static final LongAdder rollbackCount = new LongAdder();
    static final ConcurrentHashMap<String, LongAdder> rollbackCauses = new ConcurrentHashMap<>();

    public static long getCommitCount() { return commitCount.sum(); }
    public static long getRollbackCount() { return rollbackCount.sum(); }

    public static void recordRollback(Throwable ex) {
        String name = ex.getClass().getName();
        rollbackCauses.computeIfAbsent(name, k -> new LongAdder()).increment();
    }

    public static void reset() {
        commitCount.reset();
        rollbackCount.reset();
        rollbackCauses.clear();
    }
}
