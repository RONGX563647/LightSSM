package com.lightframework.tx.core;

import java.util.ArrayList;
import java.util.List;

public class TransactionSynchronizationManager {

    private static final ThreadLocal<List<TransactionSynchronization>> synchronizations =
        ThreadLocal.withInitial(ArrayList::new);

    private static final ThreadLocal<Integer> transactionDepth = ThreadLocal.withInitial(() -> 0);

    public static boolean isActive() {
        return transactionDepth.get() > 0;
    }

    public static boolean hasSynchronizations() {
        return !synchronizations.get().isEmpty();
    }

    public static void onBegin() {
        transactionDepth.set(transactionDepth.get() + 1);
    }

    public static void onEnd() {
        int depth = transactionDepth.get() - 1;
        if (depth <= 0) {
            transactionDepth.remove();
        } else {
            transactionDepth.set(depth);
        }
    }

    public static void registerSynchronization(TransactionSynchronization sync) {
        synchronizations.get().add(sync);
    }

    static List<TransactionSynchronization> getSynchronizations() {
        return synchronizations.get();
    }

    static void beforeCommit(boolean readOnly) throws Exception {
        List<TransactionSynchronization> list = synchronizations.get();
        for (TransactionSynchronization sync : list) {
            sync.beforeCommit(readOnly);
        }
    }

    static void afterCompletion(int status) {
        List<TransactionSynchronization> list = synchronizations.get();
        for (TransactionSynchronization sync : list) {
            sync.afterCompletion(status);
        }
        synchronizations.remove();
    }

    static void clear() {
        synchronizations.remove();
        transactionDepth.remove();
    }
}
