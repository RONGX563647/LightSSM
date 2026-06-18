package com.lightframework.ioc.core.health;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;

/**
 * Tarjan 强连通分量算法 — 迭代版本，找出所有循环依赖。
 * 时间复杂度：O(V+E)，空间复杂度：O(V)。
 * <p>
 * 使用显式栈替代递归 DFS，避免大型 Bean 图（1000+ beans）时的 StackOverflow 风险。
 */
public final class CycleDetector {

    private CycleDetector() {}

    /**
     * 检测所有循环依赖。
     */
    public static List<CycleInfo> detect(DependencyGraph graph) {
        int n = graph.size();
        int[] disc = new int[n];
        int[] low = new int[n];
        boolean[] onStack = new boolean[n];
        Deque<Integer> stack = new ArrayDeque<>();
        List<CycleInfo> cycles = new ArrayList<>();

        Arrays.fill(disc, -1);
        int[] timer = {0};

        for (int i = 0; i < n; i++) {
            if (disc[i] == -1) {
                tarjanIterative(i, graph, disc, low, onStack, stack, timer, cycles);
            }
        }

        return cycles;
    }

    /**
     * 迭代版 Tarjan DFS — 使用显式调用栈模拟递归。
     */
    private static void tarjanIterative(int start, DependencyGraph graph, int[] disc, int[] low,
                                         boolean[] onStack, Deque<Integer> stack,
                                         int[] timer, List<CycleInfo> cycles) {
        // 调用栈：每个帧代表一次"递归调用"
        Deque<Frame> callStack = new ArrayDeque<>();
        callStack.push(new Frame(start));

        while (!callStack.isEmpty()) {
            Frame frame = callStack.peek();
            int u = frame.u;

            if (!frame.initialized) {
                // 首次进入：初始化 disc/low
                frame.initialized = true;
                disc[u] = low[u] = timer[0]++;
                stack.push(u);
                onStack[u] = true;
            }

            int[] deps = graph.getDependencies(u);
            boolean pushedChild = false;

            while (frame.depIdx < deps.length) {
                int v = deps[frame.depIdx];
                frame.depIdx++;

                if (disc[v] == -1) {
                    // 未访问：暂停当前帧，递归访问 v
                    callStack.push(new Frame(v));
                    pushedChild = true;
                    break;
                } else if (onStack[v]) {
                    // 后向边：更新 low[u]
                    low[u] = Math.min(low[u], disc[v]);
                }
            }

            // 如果推送了子帧，等待子帧完成
            if (pushedChild) {
                continue;
            }

            // 所有邻接节点处理完毕，弹出当前帧
            callStack.pop();

            // 检查 SCC 根节点
            if (low[u] == disc[u]) {
                List<String> cycle = new ArrayList<>();
                int v;
                do {
                    v = stack.pop();
                    onStack[v] = false;
                    cycle.add(graph.getName(v));
                } while (v != u);

                if (cycle.size() > 1) {
                    cycles.add(CycleInfo.of(cycle, graph, u));
                }
            }

            // 向父帧传播 low 值
            if (!callStack.isEmpty()) {
                Frame parent = callStack.peek();
                low[parent.u] = Math.min(low[parent.u], low[u]);
            }
        }
    }

    /**
     * Tarjan DFS 调用帧 — 保存每次递归调用的局部状态。
     */
    private static final class Frame {
        final int u;          // 当前节点
        int depIdx;           // 当前处理到的邻接索引
        boolean initialized;  // 是否已初始化 disc/low

        Frame(int u) {
            this.u = u;
            this.depIdx = 0;
            this.initialized = false;
        }
    }
}
