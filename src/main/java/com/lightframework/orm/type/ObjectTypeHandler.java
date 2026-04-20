package com.rongx.mybatis.type;

import java.sql.*;

/**
 * 对象类型处理器（保底机制）
 * 用于处理未知类型，使用setObject/getObject
 */
public class ObjectTypeHandler extends BaseTypeHandler<Object> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        if (jdbcType == null) {
            ps.setObject(i, parameter);
        } else {
            ps.setObject(i, parameter, jdbcType.TYPE_CODE);
        }
    }

    @Override
    protected Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName);
    }

    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getObject(columnIndex);
    }
}