package com.lightframework.ioc.core.health;

import java.util.List;

/**
 * 循环依赖信息 — 不可变 DTO。
 */
public record CycleInfo(
    List<String> beanNames,   // 循环链中的 Bean 名称
    CycleType type,           // 循环类型
    int breakPointIndex       // 建议打断的位置（依赖度最低的 Bean）
) {
    /**
     * 循环类型
     */
    public enum CycleType {
        /** Setter/字段注入循环 — 三级缓存可解决 */
        SETTER_CYCLE,
        /** 构造器注入循环 — 需要 @Lazy 修复 */
        CONSTRUCTOR_CYCLE
    }

    /**
     * 创建 CycleInfo 实例
     */
    public static CycleInfo of(List<String> beanNames, DependencyGraph graph, int rootIndex) {
        CycleType type = determineType(beanNames, graph);
        int breakPoint = findBreakPoint(beanNames, graph);
        return new CycleInfo(beanNames, type, breakPoint);
    }

    /**
     * 判定循环类型：遍历循环中所有边，如果存在 CONSTRUCTOR 边 → CONSTRUCTOR_CYCLE
     */
    private static CycleType determineType(List<String> beanNames, DependencyGraph graph) {
        byte constructorCode = EdgeType.CONSTRUCTOR.code;
        for (int i = 0; i < beanNames.size(); i++) {
            String beanName = beanNames.get(i);
            int idx = graph.getIndex(beanName);
            if (idx == -1) continue;

            byte[] edgeTypes = graph.getEdgeTypes(idx);
            for (byte type : edgeTypes) {
                if (type == constructorCode) {
                    return CycleType.CONSTRUCTOR_CYCLE;
                }
            }
        }
        return CycleType.SETTER_CYCLE;
    }

    /**
     * 选择出度最低的 Bean 作为打断点（影响最小）
     */
    private static int findBreakPoint(List<String> beanNames, DependencyGraph graph) {
        int minOutDegree = Integer.MAX_VALUE;
        int breakPoint = 0;

        for (int i = 0; i < beanNames.size(); i++) {
            String beanName = beanNames.get(i);
            int idx = graph.getIndex(beanName);
            if (idx == -1) continue;

            int outDegree = graph.getDependencies(idx).length;
            if (outDegree < minOutDegree) {
                minOutDegree = outDegree;
                breakPoint = i;
            }
        }
        return breakPoint;
    }
}
