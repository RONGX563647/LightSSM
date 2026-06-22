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

    public static void print() {
        System.out.println("--- TransactionMetrics ---");
        System.out.println("  commits:  " + commitCount.sum());
        System.out.println("  rollbacks: " + rollbackCount.sum());
        System.out.println("  rollback causes:");
        rollbackCauses.forEach((cause, count) ->
            System.out.println("    " + cause + ": " + count.sum()));
        System.out.println("--------------------------");
    }

    public static void reset() {
        commitCount.reset();
        rollbackCount.reset();
        rollbackCauses.clear();
    }
}
