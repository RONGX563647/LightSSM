package com.lightframework.ioc.core.health;

/**
 * 依赖图边类型枚举 — 替代 magic numbers。
 */
public enum EdgeType {
    DEPENDS_ON(0),
    CONSTRUCTOR(1),
    FIELD(2),
    METHOD(3);

    public final byte code;

    EdgeType(int code) {
        this.code = (byte) code;
    }

    public static EdgeType fromCode(byte code) {
        switch (code) {
            case 0: return DEPENDS_ON;
            case 1: return CONSTRUCTOR;
            case 2: return FIELD;
            case 3: return METHOD;
            default: throw new IllegalArgumentException("Unknown edge type code: " + code);
        }
    }
}
