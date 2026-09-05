package com.lightframework.ioc.core.health;

import com.lightframework.ioc.core.DefaultListableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

/**
 * IoC 容器健康检查 — 统一入口。
 * 在 refresh() 中 registerBeanDefinitions() 之后调用。
 */
public final class IoCHealthChecker {

    private static final Logger logger = LoggerFactory.getLogger(IoCHealthChecker.class);

    private IoCHealthChecker() {}

    /**
     * 运行健康检查。
     */
    // TODO [L2][练习] 手写容器健康检查 run（构建依赖图 -> 检测循环 -> 自动修复构造器环 -> 判定是否健康）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：仅有 setter 循环依赖时判定为健康；存在未修复的构造器循环时判定为不健康并报告。
    public static HealthCheckResult run(DefaultListableBeanFactory beanFactory) {
        long start = System.currentTimeMillis();

        // 1. 构建依赖图
        DependencyGraph graph = new DependencyGraphBuilder(beanFactory).build();

        // 2. 检测循环依赖
        List<CycleInfo> cycles = CycleDetector.detect(graph);

        // 3. 自动修复构造器循环依赖
        int fixed = AutoFixer.fix(beanFactory, cycles);

        long elapsed = System.currentTimeMillis() - start;

        // 判断是否健康：无循环 或 所有循环都是 setter 类型 或 所有构造器循环都已修复
        boolean allFixed = cycles.isEmpty() ||
            cycles.stream().allMatch(c -> c.type() == CycleInfo.CycleType.SETTER_CYCLE) ||
            fixed == (int) cycles.stream()
                .filter(c -> c.type() == CycleInfo.CycleType.CONSTRUCTOR_CYCLE)
                .count();

        int totalDeps = countEdges(graph);

        return new HealthCheckResult(
            allFixed,
            beanFactory.getBeanDefinitionCount(),
            totalDeps,
            cycles,
            fixed,
            elapsed
        );
    }

    /** 统计依赖边总数 */
    private static int countEdges(DependencyGraph graph) {
        int total = 0;
        for (int i = 0; i < graph.size(); i++) {
            total += graph.getDependencies(i).length;
        }
        return total;
    }
}
