package com.lightframework.spi.schedule;

public interface ScheduledTask {
    boolean isRunning();
    void cancel();
    String getTaskId();
}
