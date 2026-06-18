package com.lightframework.orm.type;

import java.sql.*;

/**
 * 布尔类型处理器
 */
public class BooleanTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setBoolean(i, parameter);
    }

    @Override
    protected Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        boolean result = rs.getBoolean(columnName);
        return !result && rs.wasNull() ? null : result;
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        boolean result = rs.getBoolean(columnIndex);
        return !result && rs.wasNull() ? null : result;
    }
}