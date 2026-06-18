package com.lightframework.ioc.core.health;

import java.util.List;

/**
 * 健康检查报告 — 不可变 DTO。
 */
public record HealthCheckResult(
    boolean healthy,
    int totalBeans,
    int totalDependencies,
    List<CycleInfo> cycles,
    int autoFixedCount,
    long checkTimeMs
) {
    /**
     * 是否存在未修复的构造器循环依赖（致命错误）。
     */
    public boolean hasUnfixedConstructorCycles() {
        long constructorCycles = cycles.stream()
            .filter(c -> c.type() == CycleInfo.CycleType.CONSTRUCTOR_CYCLE)
            .count();
        return constructorCycles > autoFixedCount;
    }

    /**
     * 生成可读的报告字符串
     */
    public String toReportString() {
        if (healthy) {
            return "Health check passed: " + totalBeans + " beans, 0 cycles";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Health check: found ").append(cycles.size()).append(" cycles");
        sb.append(" (auto-fixed ").append(autoFixedCount).append(")");
        sb.append(" in ").append(checkTimeMs).append("ms\n");
        for (CycleInfo cycle : cycles) {
            String icon = cycle.type() == CycleInfo.CycleType.CONSTRUCTOR_CYCLE ? "[!]" : "[i]";
            sb.append("  ").append(icon)
              .append(" ").append(String.join(" -> ", cycle.beanNames()))
              .append(" -> ").append(cycle.beanNames().get(0));
            if (cycle.type() == CycleInfo.CycleType.CONSTRUCTOR_CYCLE) {
                sb.append(" [auto-fixed]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
