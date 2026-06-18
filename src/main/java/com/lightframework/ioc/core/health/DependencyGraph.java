package com.lightframework.ioc.core.health;

import java.util.Collections;
import java.util.Map;

/**
 * 不可变依赖图 — Tarjan 算法专用。
 * 性能优化：使用 int[] 邻接表而非 Map<String, Set<String>>，避免字符串查找开销。
 */
public final class DependencyGraph {

    // beanName → 索引映射（固定顺序，O(1) 查找）
    private final Map<String, Integer> nameToIndex;
    // 邻接表：adj[i] = bean i 依赖的所有 bean 索引
    private final int[][] adj;
    // 反向映射：索引 → beanName
    private final String[] indexToName;
    // 边类型：edgeType[i][jIndex] = InjectionType (CONSTRUCTOR=1/FIELD=2/METHOD=3/DEPENDS_ON=0)
    private final byte[][] edgeType;

    public DependencyGraph(Map<String, Integer> nameToIndex, int[][] adj,
                           String[] indexToName, byte[][] edgeType) {
        this.nameToIndex = nameToIndex;
        this.adj = adj;
        this.indexToName = indexToName;
        this.edgeType = edgeType;
    }

    /** Bean 数量 */
    public int size() {
        return nameToIndex.size();
    }

    /** beanName → 索引 O(1) */
    public int getIndex(String name) {
        return nameToIndex.getOrDefault(name, -1);
    }

    /** 索引 → beanName */
    public String getName(int index) {
        return indexToName[index];
    }

    /** 获取 bean i 依赖的所有 bean 索引 */
    public int[] getDependencies(int index) {
        return adj[index];
    }

    /** 获取 bean i 对应第 j 条边的类型 */
    public byte getEdgeType(int index, int jIndex) {
        return edgeType[index][jIndex];
    }

    /** 获取 bean i 的所有边类型数组 */
    public byte[] getEdgeTypes(int index) {
        return edgeType[index];
    }
}
