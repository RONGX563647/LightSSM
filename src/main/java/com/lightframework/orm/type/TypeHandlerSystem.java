package com.lightframework.orm.type;

import com.lightframework.orm.io.Resources;
import com.lightframework.orm.session.Configuration;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * 类型处理系统 - 包含所有类型处理器及相关注册功能
 */
public class TypeHandlerSystem {

    // ======================== 1. TypeHandler 接口 ========================
    public interface TypeHandler<T> {
        void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;
        T getResult(ResultSet rs, String columnName) throws SQLException;
        T getResult(ResultSet rs, int columnIndex) throws SQLException;
    }

    // ======================== 2. JdbcType 枚举 ========================
    public enum JdbcType {
        ARRAY(Types.ARRAY),
        BIT(Types.BIT),
        TINYINT(Types.TINYINT),
        SMALLINT(Types.SMALLINT),
        INTEGER(Types.INTEGER),
        BIGINT(Types.BIGINT),
        FLOAT(Types.FLOAT),
        REAL(Types.REAL),
        DOUBLE(Types.DOUBLE),
        NUMERIC(Types.NUMERIC),
        DECIMAL(Types.DECIMAL),
        CHAR(Types.CHAR),
        VARCHAR(Types.VARCHAR),
        LONGVARCHAR(Types.LONGVARCHAR),
        DATE(Types.DATE),
        TIME(Types.TIME),
        TIMESTAMP(Types.TIMESTAMP),
        BINARY(Types.BINARY),
        VARBINARY(Types.VARBINARY),
        LONGVARBINARY(Types.LONGVARBINARY),
        NULL(Types.NULL),
        OTHER(Types.OTHER),
        BLOB(Types.BLOB),
        CLOB(Types.CLOB),
        BOOLEAN(Types.BOOLEAN),
        CURSOR(-10), // Oracle
        UNDEFINED(Integer.MIN_VALUE + 1000),
        NVARCHAR(Types.NVARCHAR), // JDK6
        NCHAR(Types.NCHAR), // JDK6
        NCLOB(Types.NCLOB), // JDK6
        STRUCT(Types.STRUCT);

        public final int TYPE_CODE;
        private static final Map<Integer, JdbcType> codeLookup = new HashMap<>();

        static {
            for (JdbcType type : JdbcType.values()) {
                codeLookup.put(type.TYPE_CODE, type);
            }
        }

        JdbcType(int code) {
            this.TYPE_CODE = code;
        }

        public static JdbcType forCode(int code) {
            return codeLookup.get(code);
        }
    }

    // ======================== 3. 简单类型注册表 ========================
    public static class SimpleTypeRegistry {
        private static final Set<Class<?>> SIMPLE_TYPE_SET = new HashSet<>();

        static {
            SIMPLE_TYPE_SET.add(String.class);
            SIMPLE_TYPE_SET.add(Byte.class);
            SIMPLE_TYPE_SET.add(Short.class);
            SIMPLE_TYPE_SET.add(Character.class);
            SIMPLE_TYPE_SET.add(Integer.class);
            SIMPLE_TYPE_SET.add(Long.class);
            SIMPLE_TYPE_SET.add(Float.class);
            SIMPLE_TYPE_SET.add(Double.class);
            SIMPLE_TYPE_SET.add(Boolean.class);
            SIMPLE_TYPE_SET.add(Date.class);
            SIMPLE_TYPE_SET.add(Class.class);
            SIMPLE_TYPE_SET.add(BigInteger.class);
            SIMPLE_TYPE_SET.add(BigDecimal.class);
            SIMPLE_TYPE_SET.add(byte[].class);
            SIMPLE_TYPE_SET.add(Byte[].class);
            SIMPLE_TYPE_SET.add(java.sql.Date.class);
            SIMPLE_TYPE_SET.add(java.sql.Time.class);
            SIMPLE_TYPE_SET.add(java.sql.Timestamp.class);
        }

        private SimpleTypeRegistry() {}

        public static boolean isSimpleType(Class<?> clazz) {
            return SIMPLE_TYPE_SET.contains(clazz);
        }
    }

    // ======================== 4. 类型别名注册表 ========================
    public static class TypeAliasRegistry {
        private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<>();

        public TypeAliasRegistry() {
            // 基本类型
            registerAlias("string", String.class);
            registerAlias("byte", Byte.class);
            registerAlias("short", Short.class);
            registerAlias("int", Integer.class);
            registerAlias("integer", Integer.class);
            registerAlias("long", Long.class);
            registerAlias("float", Float.class);
            registerAlias("double", Double.class);
            registerAlias("boolean", Boolean.class);
            registerAlias("char", Character.class);

            // 数值类型
            registerAlias("decimal", BigDecimal.class);
            registerAlias("bigdecimal", BigDecimal.class);
            registerAlias("biginteger", BigInteger.class);

            // 日期类型
            registerAlias("date", Date.class);
            registerAlias("sqldate", java.sql.Date.class);
            registerAlias("sqltime", java.sql.Time.class);
            registerAlias("sqltimestamp", java.sql.Timestamp.class);

            // 数组类型
            registerAlias("byte[]", byte[].class);
            registerAlias("Byte[]", Byte[].class);

            // 对象类型
            registerAlias("object", Object.class);
            registerAlias("map", Map.class);
            registerAlias("hashmap", HashMap.class);
            registerAlias("list", List.class);
            registerAlias("arraylist", ArrayList.class);
            registerAlias("collection", Collection.class);
            registerAlias("iterator", Iterator.class);
        }

        public void registerAlias(String alias, Class<?> value) {
            String key = alias.toLowerCase(Locale.ENGLISH);
            TYPE_ALIASES.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> Class<T> resolveAlias(String alias) {
            try {
                if (alias == null) return null;
                String key = alias.toLowerCase(Locale.ENGLISH);
                Class<T> value;
                if (TYPE_ALIASES.containsKey(key)) {
                    value = (Class<T>) TYPE_ALIASES.get(key);
                } else {
                    value = (Class<T>) Resources.classForName(alias);
                }
                return value;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("无法解析类型别名 '" + alias + "'", e);
            }
        }
    }

    // ======================== 5. 类型处理器注册表 ========================
    public static class TypeHandlerRegistry {
        private final Map<JdbcType, TypeHandler<?>> JDBC_TYPE_HANDLER_MAP = new EnumMap<>(JdbcType.class);
        private final Map<Type, Map<JdbcType, TypeHandler<?>>> TYPE_HANDLER_MAP = new HashMap<>();
        private final Map<Class<?>, TypeHandler<?>> ALL_TYPE_HANDLERS_MAP = new HashMap<>();
        private final TypeHandler<Object> UNKNOWN_TYPE_HANDLER = new UnknownTypeHandler();

        public TypeHandlerRegistry() {
            // 注册所有内置类型处理器
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
            register(Character.class, new CharacterTypeHandler());
            register(char.class, new CharacterTypeHandler());
            register(String.class, new StringTypeHandler());
            register(String.class, JdbcType.CHAR, new StringTypeHandler());
            register(String.class, JdbcType.VARCHAR, new StringTypeHandler());
            register(Date.class, new DateTypeHandler());
            register(java.sql.Date.class, new SqlDateTypeHandler());
            register(java.sql.Time.class, new SqlTimeTypeHandler());
            register(java.sql.Timestamp.class, new SqlTimestampTypeHandler());
            register(BigDecimal.class, new BigDecimalTypeHandler());
            register(BigInteger.class, new BigIntegerTypeHandler());
            register(byte[].class, new ByteArrayTypeHandler());
//            register(Byte[].class, new ByteArrayTypeHandler());
        }

        private <T> void register(Class<T> javaType, TypeHandler<? extends T> typeHandler) {
            register(javaType, null, typeHandler);
        }

        private <T> void register(Class<T> javaType, JdbcType jdbcType, TypeHandler<? extends T> typeHandler) {
            if (javaType != null) {
                Map<JdbcType, TypeHandler<?>> map = TYPE_HANDLER_MAP.computeIfAbsent(javaType, k -> new HashMap<>());
                map.put(jdbcType, typeHandler);
            }
            ALL_TYPE_HANDLERS_MAP.put(typeHandler.getClass(), typeHandler);
        }

        @SuppressWarnings("unchecked")
        public <T> TypeHandler<T> getTypeHandler(Class<T> type, JdbcType jdbcType) {
            Map<JdbcType, TypeHandler<?>> jdbcHandlerMap = TYPE_HANDLER_MAP.get(type);
            TypeHandler<?> handler = null;
            if (jdbcHandlerMap != null) {
                handler = jdbcHandlerMap.get(jdbcType);
                if (handler == null) {
                    handler = jdbcHandlerMap.get(null);
                }
            }
            if (handler == null) {
                handler = (TypeHandler<T>) UNKNOWN_TYPE_HANDLER;
            }
            return (TypeHandler<T>) handler;
        }

        public boolean hasTypeHandler(Class<?> javaType) {
            return hasTypeHandler(javaType, null);
        }

        public boolean hasTypeHandler(Class<?> javaType, JdbcType jdbcType) {
            return javaType != null && getTypeHandler(javaType, jdbcType) != null;
        }

        public TypeHandler<?> getMappingTypeHandler(Class<? extends TypeHandler<?>> handlerType) {
            return ALL_TYPE_HANDLERS_MAP.get(handlerType);
        }
    }

    // ======================== 6. 基础类型处理器 ========================
    public abstract static class BaseTypeHandler<T> implements TypeHandler<T> {
        protected Configuration configuration;

        public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
            if (parameter == null) {
                if (jdbcType == null) {
                    ps.setNull(i, Types.OTHER);
                } else {
                    ps.setNull(i, jdbcType.TYPE_CODE);
                }
            } else {
                setNonNullParameter(ps, i, parameter, jdbcType);
            }
        }

        @Override
        public T getResult(ResultSet rs, String columnName) throws SQLException {
            return getNullableResult(rs, columnName);
        }

        @Override
        public T getResult(ResultSet rs, int columnIndex) throws SQLException {
            return getNullableResult(rs, columnIndex);
        }

        protected abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
                throws SQLException;

        protected abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

        protected abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;
    }

    // ======================== 7. 具体类型处理器实现 ========================

    // Boolean 类型处理器
    public static class BooleanTypeHandler extends BaseTypeHandler<Boolean> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setBoolean(i, parameter);
        }

        @Override
        protected Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
            boolean result = rs.getBoolean(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            boolean result = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Byte 类型处理器
    public static class ByteTypeHandler extends BaseTypeHandler<Byte> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Byte parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setByte(i, parameter);
        }

        @Override
        protected Byte getNullableResult(ResultSet rs, String columnName) throws SQLException {
            byte result = rs.getByte(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Byte getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            byte result = rs.getByte(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Short 类型处理器
    public static class ShortTypeHandler extends BaseTypeHandler<Short> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Short parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setShort(i, parameter);
        }

        @Override
        protected Short getNullableResult(ResultSet rs, String columnName) throws SQLException {
            short result = rs.getShort(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Short getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            short result = rs.getShort(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Integer 类型处理器
    public static class IntegerTypeHandler extends BaseTypeHandler<Integer> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Integer parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setInt(i, parameter);
        }

        @Override
        protected Integer getNullableResult(ResultSet rs, String columnName) throws SQLException {
            int result = rs.getInt(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Integer getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            int result = rs.getInt(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Long 类型处理器
    public static class LongTypeHandler extends BaseTypeHandler<Long> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setLong(i, parameter);
        }

        @Override
        protected Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
            long result = rs.getLong(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            long result = rs.getLong(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Float 类型处理器
    public static class FloatTypeHandler extends BaseTypeHandler<Float> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Float parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setFloat(i, parameter);
        }

        @Override
        protected Float getNullableResult(ResultSet rs, String columnName) throws SQLException {
            float result = rs.getFloat(columnName);
            return rs.wasNull() ? null : result;
        }

        @Override
        protected Float getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            float result = rs.getFloat(columnIndex);
            return rs.wasNull() ? null : result;
        }
    }

    // Double 类型处理器
    public static class DoubleTypeHandler extends BaseTypeHandler<Double> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Double parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setDouble(i, parameter);
        }

        @Override
        protected Double getNullableResult(ResultSet rs, String columnName) throws SQLException {
            double result = rs.getDouble(columnName);
            return result == 0 && rs.wasNull() ? null : result;
        }

        @Override
        protected Double getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            double result = rs.getDouble(columnIndex);
            return result == 0 && rs.wasNull() ? null : result;
        }
    }

    // Character 类型处理器
    public static class CharacterTypeHandler extends BaseTypeHandler<Character> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Character parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, parameter.toString());
        }

        @Override
        protected Character getNullableResult(ResultSet rs, String columnName) throws SQLException {
            String result = rs.getString(columnName);
            return result == null || result.length() == 0 ? null : result.charAt(0);
        }

        @Override
        protected Character getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            String result = rs.getString(columnIndex);
            return result == null || result.length() == 0 ? null : result.charAt(0);
        }
    }

    // String 类型处理器
    public static class StringTypeHandler extends BaseTypeHandler<String> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, parameter);
        }

        @Override
        protected String getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getString(columnName);
        }

        @Override
        protected String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getString(columnIndex);
        }
    }

    // Date 类型处理器 (java.util.Date)
    public static class DateTypeHandler extends BaseTypeHandler<Date> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Date parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setTimestamp(i, new Timestamp(parameter.getTime()));
        }

        @Override
        protected Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? new Date(timestamp.getTime()) : null;
        }

        @Override
        protected Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            Timestamp timestamp = rs.getTimestamp(columnIndex);
            return timestamp != null ? new Date(timestamp.getTime()) : null;
        }
    }

    // SqlDate 类型处理器 (java.sql.Date)
    public static class SqlDateTypeHandler extends BaseTypeHandler<java.sql.Date> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, java.sql.Date parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setDate(i, parameter);
        }

        @Override
        protected java.sql.Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getDate(columnName);
        }

        @Override
        protected java.sql.Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getDate(columnIndex);
        }
    }

    // SqlTime 类型处理器 (java.sql.Time)
    public static class SqlTimeTypeHandler extends BaseTypeHandler<Time> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Time parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setTime(i, parameter);
        }

        @Override
        protected Time getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getTime(columnName);
        }

        @Override
        protected Time getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getTime(columnIndex);
        }
    }

    // SqlTimestamp 类型处理器 (java.sql.Timestamp)
    public static class SqlTimestampTypeHandler extends BaseTypeHandler<Timestamp> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Timestamp parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setTimestamp(i, parameter);
        }

        @Override
        protected Timestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getTimestamp(columnName);
        }

        @Override
        protected Timestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getTimestamp(columnIndex);
        }
    }

    // BigDecimal 类型处理器
    public static class BigDecimalTypeHandler extends BaseTypeHandler<BigDecimal> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, BigDecimal parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setBigDecimal(i, parameter);
        }

        @Override
        protected BigDecimal getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getBigDecimal(columnName);
        }

        @Override
        protected BigDecimal getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getBigDecimal(columnIndex);
        }
    }

    // BigInteger 类型处理器
    public static class BigIntegerTypeHandler extends BaseTypeHandler<BigInteger> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, BigInteger parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setString(i, parameter.toString());
        }

        @Override
        protected BigInteger getNullableResult(ResultSet rs, String columnName) throws SQLException {
            String result = rs.getString(columnName);
            return result != null ? new BigInteger(result) : null;
        }

        @Override
        protected BigInteger getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            String result = rs.getString(columnIndex);
            return result != null ? new BigInteger(result) : null;
        }
    }

    // ByteArray 类型处理器
    public static class ByteArrayTypeHandler extends BaseTypeHandler<byte[]> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType)
                throws SQLException {
            ps.setBytes(i, parameter);
        }

        @Override
        protected byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getBytes(columnName);
        }

        @Override
        protected byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getBytes(columnIndex);
        }
    }

    // Unknown 类型处理器 (兜底处理器)
    public static class UnknownTypeHandler extends BaseTypeHandler<Object> {
        @Override
        protected void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType)
                throws SQLException {
            if (parameter instanceof String) {
                ps.setString(i, (String) parameter);
            } else if (parameter instanceof Integer) {
                ps.setInt(i, (Integer) parameter);
            } else if (parameter instanceof Long) {
                ps.setLong(i, (Long) parameter);
            } else if (parameter instanceof Double) {
                ps.setDouble(i, (Double) parameter);
            } else if (parameter instanceof Float) {
                ps.setFloat(i, (Float) parameter);
            } else if (parameter instanceof Boolean) {
                ps.setBoolean(i, (Boolean) parameter);
            } else if (parameter instanceof Date) {
                ps.setTimestamp(i, new Timestamp(((Date) parameter).getTime()));
            } else if (parameter instanceof java.sql.Date) {
                ps.setDate(i, (java.sql.Date) parameter);
            } else if (parameter instanceof Timestamp) {
                ps.setTimestamp(i, (Timestamp) parameter);
            } else if (parameter instanceof byte[]) {
                ps.setBytes(i, (byte[]) parameter);
            } else {
                // 默认使用toString()
                ps.setString(i, parameter.toString());
            }
        }

        @Override
        protected Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
            return rs.getObject(columnName);
        }

        @Override
        protected Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
            return rs.getObject(columnIndex);
        }
    }

    // ======================== 8. 工具方法 ========================

    /**
     * 获取所有注册的类型处理器类
     */
    public static List<Class<? extends TypeHandler<?>>> getAllTypeHandlerClasses() {
        return Arrays.asList(
                BooleanTypeHandler.class,
                ByteTypeHandler.class,
                ShortTypeHandler.class,
                IntegerTypeHandler.class,
                LongTypeHandler.class,
                FloatTypeHandler.class,
                DoubleTypeHandler.class,
                CharacterTypeHandler.class,
                StringTypeHandler.class,
                DateTypeHandler.class,
                SqlDateTypeHandler.class,
                SqlTimeTypeHandler.class,
                SqlTimestampTypeHandler.class,
                BigDecimalTypeHandler.class,
                BigIntegerTypeHandler.class,
                ByteArrayTypeHandler.class,
                UnknownTypeHandler.class
        );
    }

    /**
     * 创建默认的类型处理器注册表
     */
    public static TypeHandlerRegistry createDefaultTypeHandlerRegistry() {
        return new TypeHandlerRegistry();
    }

    /**
     * 创建默认的类型别名注册表
     */
    public static TypeAliasRegistry createDefaultTypeAliasRegistry() {
        return new TypeAliasRegistry();
    }

    /**
     * 检查是否为简单类型
     */
    public static boolean isSimpleType(Class<?> clazz) {
        return SimpleTypeRegistry.isSimpleType(clazz);
    }
}