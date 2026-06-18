package com.lightframework.orm.type;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * 类型处理器注册表
 * 优化：添加保底机制，支持更多常用类型，包括Java 8日期时间API
 */
public final class TypeHandlerRegistry {

    private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<>(JdbcType.class);
    private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new HashMap<>();
    private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<>();

    // 默认类型处理器（保底机制）
    private final TypeHandler<Object> DEFAULT_TYPE_HANDLER = new ObjectTypeHandler();
    private final TypeHandler<String> DEFAULT_STRING_TYPE_HANDLER = new StringTypeHandler();

    public TypeHandlerRegistry() {
        // 注册原始类型及其包装类
        register(Boolean.class, new BooleanTypeHandler());
        register(boolean.class, new BooleanTypeHandler());

        register(Byte.class, new ByteTypeHandler());
        register(byte.class, new ByteTypeHandler());

        register(Short.class, new ShortTypeHandler());
        register(short.class, new ShortTypeHandler());

        register(Integer.class, new IntegerTypeHandler());
        register(int.class, new IntegerTypeHandler());

        register(Long.class, new LongTypeHandler());
        register(long.class, new LongTypeHandler());

        register(Float.class, new FloatTypeHandler());
        register(float.class, new FloatTypeHandler());

        register(Double.class, new DoubleTypeHandler());
        register(double.class, new DoubleTypeHandler());

        // 注册其他常用类型
        register(String.class, new StringTypeHandler());
        register(String.class, JdbcType.CHAR, new StringTypeHandler());
        register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
        register(String.class, JdbcType.LONGVARCHAR, new StringTypeHandler());

        register(Date.class, new DateTypeHandler());
        register(java.sql.Date.class, new SqlDateTypeHandler());
        register(Time.class, new SqlTimeTypeHandler());
        register(Timestamp.class, new SqlTimestampTypeHandler());

        // 注册Java 8日期时间API
        register(LocalDate.class, new LocalDateTypeHandler());
        register(LocalTime.class, new LocalTimeTypeHandler());
        register(LocalDateTime.class, new LocalDateTimeTypeHandler());
        register(Instant.class, new InstantTypeHandler());

        // 注册大数字类型
        register(BigDecimal.class, new BigDecimalTypeHandler());
        register(BigInteger.class, new BigIntegerTypeHandler());

        // 注册数组类型
        register(byte[].class, new ByteArrayTypeHandler());
        register(Byte[].class, new ByteObjectArrayTypeHandler());

        // 注册枚举类型
        register(Enum.class, new EnumTypeHandler());

        // 注册Blob/Clob类型
        register(Blob.class, new BlobTypeHandler());
//        register(Clob.class, new ClobTypeHandler());

        // 注册JDBC类型到默认处理器
        registerDefaultJdbcTypeHandlers();
    }

    private void registerDefaultJdbcTypeHandlers() {
        // 为常见的JDBC类型注册默认处理器
        register(JdbcType.CHAR, DEFAULT_STRING_TYPE_HANDLER);
        register(JdbcType.VARCHAR, DEFAULT_STRING_TYPE_HANDLER);
        register(JdbcType.LONGVARCHAR, DEFAULT_STRING_TYPE_HANDLER);
        register(JdbcType.NUMERIC, new BigDecimalTypeHandler());
        register(JdbcType.DECIMAL, new BigDecimalTypeHandler());
        register(JdbcType.BIT, new BooleanTypeHandler());
//        register(JdbcType.TINYINT, new ByteTypeHandler());
//        register(JdbcType.SMALLINT, new TypeHandlerSystem.ShortTypeHandler());
        register(JdbcType.INTEGER, new IntegerTypeHandler());
        register(JdbcType.BIGINT, new LongTypeHandler());
        register(JdbcType.REAL, new FloatTypeHandler());
        register(JdbcType.FLOAT, new FloatTypeHandler());
        register(JdbcType.DOUBLE, new DoubleTypeHandler());
        register(JdbcType.BINARY, new ByteArrayTypeHandler());
        register(JdbcType.VARBINARY, new ByteArrayTypeHandler());
        register(JdbcType.LONGVARBINARY, new ByteArrayTypeHandler());
        register(JdbcType.DATE, new SqlDateTypeHandler());
        register(JdbcType.TIME, new SqlTimeTypeHandler());
//        register(JdbcType.TIMESTAMP, new SqlTimestampTypeHandler());
        register(JdbcType.BLOB, new BlobTypeHandler());
//        register(JdbcType.CLOB, newClobTypeHandler());
    }

    private <T> void register(Type javaType, TypeHandler<? extends T> typeHandler) {
        register(javaType, null, typeHandler);
    }

    public void register(JdbcType jdbcType, TypeHandler<?> handler) {
        JDBC_TYPE_HANDLER_MAP.put(jdbcType, handler);
    }

    private void register(Type javaType, JdbcType jdbcType, TypeHandler<?> handler) {
        if (javaType != null) {
            Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.computeIfAbsent(javaType, k -> new HashMap<>());
            map.put(jdbcType, handler);
        }
        if (handler != null) {
            ALL_TYPE_HANDLERS_MAP.put(handler.getClass(), handler);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
        return getTypeHandler((Type) type, jdbcType);
    }

    public boolean hasTypeHandler(Class<?> javaType) {
        return hasTypeHandler(javaType, null);
    }

    public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
        return javaType != null && getTypeHandler((Type) javaType, jdbcType) != null;
    }

    @SuppressWarnings("unchecked")
    private <T> TypeHandler<T> getTypeHandler(Type type, JdbcType jdbcType) {
        // 1. 首先尝试精确匹配
        Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
        TypeHandler<?> handler = null;

        if (jdbcHandlerMap != null) {
            handler = jdbcHandlerMap.get(jdbcType);
            if (handler == null) {
                handler = jdbcHandlerMap.get(null); // 无特定JdbcType的处理器
            }
        }

        // 2. 如果没找到，尝试父类或接口
        if (handler == null && type instanceof Class) {
            Class<?> clazz = (Class<?>) type;

            // 检查枚举类型
            if (Enum.class.isAssignableFrom(clazz)) {
                handler = TYPE_HANDLER_MAP.get(Enum.class).get(jdbcType);
            }

            // 检查父类
            if (handler == null && clazz.getSuperclass() != null) {
                handler = getTypeHandler(clazz.getSuperclass(), jdbcType);
            }

            // 检查接口
            if (handler == null) {
                for (Class<?> iface : clazz.getInterfaces()) {
                    handler = getTypeHandler(iface, jdbcType);
                    if (handler != null) break;
                }
            }
        }

        // 3. 如果还没找到，尝试根据JdbcType获取默认处理器
        if (handler == null && jdbcType != null) {
            handler = JDBC_TYPE_HANDLER_MAP.get(jdbcType);
        }

        // 4. 最后使用默认的ObjectTypeHandler（保底机制）
        if (handler == null) {
            handler = DEFAULT_TYPE_HANDLER;
        }

        return (TypeHandler<T>) handler;
    }

    public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
        return ALL_TYPE_HANDLERS_MAP.get(handlerType);
    }

    /**
     * 注册自定义类型处理器
     */
    public <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
        register((Type) javaType, typeHandler);
    }

    /**
     * 获取默认类型处理器（保底机制）
     */
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> getDefaultTypeHandler() {
        return (TypeHandler<T>) DEFAULT_TYPE_HANDLER;
    }
}