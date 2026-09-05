package com.lightframework.ioc.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 快速 Bean 查找器
 * 使用 LinkedHashMap 保证插入顺序，缓存前32个Bean
 * 使用数组实现 O(1) 查找，避免 HashMap.get() 开销
 */
public class FastBeanLookup {

    private volatile FastBeanTable table;

    private static class FastBeanTable {
        final int[] hashes;
        final String[] names;
        final Object[] beans;

        FastBeanTable(int[] h, String[] n, Object[] b) {
            this.hashes = h;
            this.names = n;
            this.beans = b;
        }
    }

    /**
     * 构建快速查找表
     * 使用 LinkedHashMap 保证遍历顺序可预测
     */
    public void build(Map<String, Object> frozen) {
        int size = Math.min(frozen.size(), 32);
        int[] hashes = new int[size];
        String[] names = new String[size];
        Object[] beans = new Object[size];
        int i = 0;
        // 使用 LinkedHashMap 确保遍历顺序一致
        for (Map.Entry<String, Object> e : frozen.entrySet()) {
            if (i >= size) break;
            hashes[i] = e.getKey().hashCode();
            names[i] = e.getKey();
            beans[i] = e.getValue();
            i++;
        }
        this.table = new FastBeanTable(hashes, names, beans);
    }

    /**
     * 快速查找 Bean（O(1) 数组线性探测）
     * @return null 表示不在快速路径中
     */
    // TODO [L1][练习] 手写 O(1) 快速查找 lookup（用开放寻址哈希表替代当前小数组线性扫描，正确处理 hash 冲突）。；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   验收标准：build 后 lookup 对任意已注册 name 返回正确 bean；冲突 key 仍能命中。
    public Object lookup(String name) {
        FastBeanTable t = this.table;
        if (t == null) return null;

        int hash = name.hashCode();
        // 小数组线性搜索快于 HashMap（避免 hashCode 冲突处理开销）
        for (int i = 0; i < t.hashes.length; i++) {
            if (t.hashes[i] == hash && t.names[i].equals(name)) {
                return t.beans[i];
            }
        }
        return null;
    }

    /**
     * 清空快速查找表
     */
    public void clear() {
        this.table = null;
    }

    /**
     * 是否已构建
     */
    public boolean isBuilt() {
        return this.table != null;
    }
}
