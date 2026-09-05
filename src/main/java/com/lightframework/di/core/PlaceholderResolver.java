package com.lightframework.di.core;

// TODO [L1][练习] 手写一个 PlaceholderResolver 实现——从 Properties/环境变量/System.getProperties 中按 key 读取 ${key:default} 的值，支持「冒号默认值」语法（key 不存在时回退到 default）；写对标志：resolvePlaceholder("app.name") 返回配置值，resolvePlaceholder("miss:def") 返回 "def"。
@FunctionalInterface
public interface PlaceholderResolver {
    String resolvePlaceholder(String placeholder);
}
