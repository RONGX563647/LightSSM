package com.lightframework.orm.type;

import java.sql.*;

/**
 * java.sql.Date类型处理器
 */
public class SqlDateTypeHandler extends BaseTypeHandler<java.sql.Date> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, java.sql.Date parameter, JdbcType jdbcType) throws SQLException {
        ps.setDate(i, parameter);
    }

    @Override
    protected java.sql.Date getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getDate(columnName);
    }

    @Override
    public java.sql.Date getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getDate(columnIndex);
    }
}