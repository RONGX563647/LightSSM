package com.lightframework.ioc.core.health;

import com.lightframework.di.core.AnnotationInjectEntry;
import com.lightframework.di.core.InjectionMetadata;
import com.lightframework.ioc.beans.BeanDefinition;
import com.lightframework.ioc.core.DefaultListableBeanFactory;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 BeanDefinition 和 InjectionMetadata 构建完整依赖图。
 * 复用已缓存的 InjectionMetadata，零额外反射。
 * <p>
 * 性能优化：使用 BeanFactory.typeIndex 进行 O(1) 类型查找，
 * 替代原有的 O(V) 全量遍历。
 */
public final class DependencyGraphBuilder {

    private final DefaultListableBeanFactory beanFactory;

    public DependencyGraphBuilder(DefaultListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * 构建依赖图 — 入口方法。
     * 时间复杂度：O(V + E)，V=bean 数量，E=依赖边数量。
     */
    // TODO [L3][练习] 手写依赖图构建 build（建立 name->index 映射，遍历每个 Bean 的 @DependsOn/字段/构造器依赖生成邻接表，区分边类型）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：对 A 依赖 B 的图，adj[A] 包含 B 的索引且边类型正确。
    public DependencyGraph build() {
        String[] beanNames = beanFactory.getBeanDefinitionNames();
        int n = beanNames.length;
        if (n == 0) {
            return new DependencyGraph(Map.of(), new int[0][], new String[0], new byte[0][]);
        }

        // Step 1: 建立名称-索引映射
        Map<String, Integer> nameToIndex = new HashMap<>(n);
        String[] indexToName = new String[n];
        for (int i = 0; i < n; i++) {
            nameToIndex.put(beanNames[i], i);
            indexToName[i] = beanNames[i];
        }

        // Step 2: 构建邻接表 + 边类型
        int[][] adj = new int[n][];
        byte[][] edgeTypes = new byte[n][];

        for (int i = 0; i < n; i++) {
            List<Integer> depList = new ArrayList<>(4);
            List<Byte> typeList = new ArrayList<>(4);
            collectDependencies(beanNames[i], i, nameToIndex, depList, typeList);

            int depCount = depList.size();
            adj[i] = new int[depCount];
            edgeTypes[i] = new byte[depCount];
            for (int j = 0; j < depCount; j++) {
                adj[i][j] = depList.get(j);
                edgeTypes[i][j] = typeList.get(j);
            }
        }

        return new DependencyGraph(nameToIndex, adj, indexToName, edgeTypes);
    }

    /**
     * 收集单个 Bean 的所有依赖 — 复用 InjectionMetadata 缓存。
     */
    private void collectDependencies(String beanName, int selfIdx, Map<String, Integer> nameToIndex,
                                      List<Integer> depList, List<Byte> typeList) {
        BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
        if (bd == null) return;
        Class<?> beanClass = bd.getBeanClass();
        if (beanClass == null) return;

        // 1. @DependsOn 显式依赖
        String[] dependsOn = bd.getDependsOn();
        if (dependsOn != null) {
            for (String dep : dependsOn) {
                addNamedDependency(dep, selfIdx, nameToIndex, depList, typeList, EdgeType.DEPENDS_ON);
            }
        }

        // 2. 收集注解元数据（即时收集，不触发 Bean 实例化）
        InjectionMetadata meta = resolveMetadata(beanClass);
        if (meta != null) {
            // @Autowired 字段依赖
            for (AnnotationInjectEntry entry : meta.autowiredEntries) {
                if (entry.field != null) {
                    addTypeDependency(entry.field.getType(), selfIdx, nameToIndex, depList, typeList, EdgeType.FIELD);
                }
            }
            // @Resource 字段依赖
            for (AnnotationInjectEntry entry : meta.resourceEntries) {
                if (entry.resourceName != null) {
                    addNamedDependency(entry.resourceName, selfIdx, nameToIndex, depList, typeList, EdgeType.FIELD);
                } else {
                    addTypeDependency(entry.type, selfIdx, nameToIndex, depList, typeList, EdgeType.FIELD);
                }
            }
        }

        // 3. 构造器参数依赖
        collectConstructorDependencies(beanClass, selfIdx, nameToIndex, depList, typeList);
    }

    /**
     * 按名称添加依赖边。
     */
    private void addNamedDependency(String depName, int selfIdx, Map<String, Integer> nameToIndex,
                                     List<Integer> depList, List<Byte> typeList, EdgeType edgeType) {
        Integer idx = nameToIndex.get(depName);
        if (idx != null && idx != selfIdx) {
            depList.add(idx);
            typeList.add(edgeType.code);
        }
    }

    /**
     * 根据类型添加依赖边 — O(1) 使用 BeanFactory.typeIndex。
     */
    private void addTypeDependency(Class<?> type, int selfIdx, Map<String, Integer> nameToIndex,
                                    List<Integer> depList, List<Byte> typeList, EdgeType edgeType) {
        if (type == null || type.isPrimitive() || type.isArray()) return;

        // ★ 使用 typeIndex O(1) 查找，替代全量遍历 O(V)
        String[] matchingBeans = beanFactory.getBeanNamesForType(type);
        if (matchingBeans.length > 0) {
            Integer idx = nameToIndex.get(matchingBeans[0]);
            if (idx != null && idx != selfIdx) {
                depList.add(idx);
                typeList.add(edgeType.code);
            }
        }
    }

    /**
     * 即时收集注解元数据（不触发 Bean 实例化）。
     * 如果缓存中没有，即时解析。
     */
    private InjectionMetadata resolveMetadata(Class<?> beanClass) {
        // 尝试从缓存获取
        InjectionMetadata meta = beanFactory.getInjectionMetadata(beanClass);
        if (meta != null) {
            return meta;
        }
        // 缓存未命中，即时解析（不触发 Bean 实例化）
        meta = new InjectionMetadata();
        beanFactory.resolveInjectionMetadata(beanClass, meta);
        return meta;
    }

    /**
     * 收集构造器参数依赖。
     */
    private void collectConstructorDependencies(Class<?> beanClass, int selfIdx,
                                                 Map<String, Integer> nameToIndex,
                                                 List<Integer> depList, List<Byte> typeList) {
        Constructor<?> targetCtor = findTargetConstructor(beanClass);
        if (targetCtor == null) return;

        Class<?>[] paramTypes = targetCtor.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            addTypeDependency(paramType, selfIdx, nameToIndex, depList, typeList, EdgeType.CONSTRUCTOR);
        }
    }

    /**
     * 查找目标构造器：优先 @Autowired 构造器，否则选参数最多的。
     */
    static Constructor<?> findTargetConstructor(Class<?> beanClass) {
        Constructor<?>[] ctors = beanClass.getDeclaredConstructors();
        if (ctors.length == 0) return null;

        // 优先查找 @Autowired 构造器
        Constructor<?> autowiredCtor = null;
        Constructor<?> maxParamCtor = ctors[0];
        for (Constructor<?> ctor : ctors) {
            if (ctor.isAnnotationPresent(com.lightframework.di.annotation.Autowired.class)) {
                autowiredCtor = ctor;
                break;
            }
            if (ctor.getParameterCount() > maxParamCtor.getParameterCount()) {
                maxParamCtor = ctor;
            }
        }
        return autowiredCtor != null ? autowiredCtor : maxParamCtor;
    }
}
