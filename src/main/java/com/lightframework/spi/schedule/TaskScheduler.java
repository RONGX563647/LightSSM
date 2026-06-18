package com.lightframework.spi.schedule;

public interface TaskScheduler {
    ScheduledTask schedule(Runnable task, String cronExpression);

    ScheduledTask scheduleAtFixedRate(Runnable task, long intervalMs);

    ScheduledTask scheduleWithDelay(Runnable task, long delayMs);

    void cancelAll();

    boolean isStarted();
}
