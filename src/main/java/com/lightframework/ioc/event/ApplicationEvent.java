package com.lightframework.ioc.event;

/**
 * Base class for application events, similar to Spring Framework's ApplicationEvent.
 * All custom events should extend this class.
 */
public abstract class ApplicationEvent {
    private final Object source;
    private final long timestamp;
    
    public ApplicationEvent(Object source) {
        this.source = source;
        this.timestamp = System.currentTimeMillis();
    }
    
    // TODO [L1][练习] 手写 ApplicationEvent 的 equals/hashCode（按 source + timestamp 比较；或仅按 source，便于测试断言）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：两个相同 source 的事件 equals 为 true（按需实现）。
    public Object getSource() {
        return source;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}
