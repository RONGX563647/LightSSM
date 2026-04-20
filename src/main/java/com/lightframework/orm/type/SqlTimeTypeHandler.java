package com.rongx.mybatis.type;

import java.sql.*;

/**
 * java.sql.Time类型处理器
 */
public class SqlTimeTypeHandler extends BaseTypeHandler<Time> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Time parameter, JdbcType jdbcType) throws SQLException {
        ps.setTime(i, parameter);
    }

    @Override
    protected Time getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTime(columnName);
    }

    @Override
    public Time getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTime(columnIndex);
    }
}