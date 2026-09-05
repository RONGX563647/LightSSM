package com.lightframework.ioc.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 SimpleApplicationEventMulticaster 的事件广播（精确匹配 + 父类事件类型匹配）。
 */
public class EventMulticasterTest {

    static class MyEvent extends ApplicationEvent {
        MyEvent(String msg) {
            super(msg);
        }
    }

    @Test
    void publishToMatchingListeners() {
        // 验证 TODO[L2] 练习目标：手写事件广播 —— 精确匹配与父类匹配监听器都被触发
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        StringBuilder sb = new StringBuilder();
        multicaster.addListener(new ApplicationListener<MyEvent>() {
            @Override
            public void onApplicationEvent(MyEvent event) {
                sb.append("my:");
            }
        });
        multicaster.addListener(new ApplicationListener<ApplicationEvent>() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
                sb.append("base:");
            }
        });

        multicaster.publishEvent(new MyEvent("hi"));
        assertTrue(sb.toString().contains("my:"));
        assertTrue(sb.toString().contains("base:"));
    }
}
