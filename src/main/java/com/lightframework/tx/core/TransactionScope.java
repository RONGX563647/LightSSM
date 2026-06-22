package com.lightframework.tx.core;

import com.lightframework.tx.annotation.Propagation;

public class TransactionScope {

    private static final ThreadLocal<Propagation> override = new ThreadLocal<>();

    public static Propagation getOverride() {
        return override.get();
    }

    public static void within(Propagation propagation, Runnable action) {
        Propagation previous = override.get();
        override.set(propagation);
        try {
            action.run();
        } finally {
            override.set(previous);
        }
    }
}
