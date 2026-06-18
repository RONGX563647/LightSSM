package com.lightframework.orm.type;

import java.sql.*;

/**
 * Short类型处理器
 */
public class ShortTypeHandler extends BaseTypeHandler<Short> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Short parameter, JdbcType jdbcType) throws SQLException {
        ps.setShort(i, parameter);
    }

    @Override
    protected Short getNullableResult(ResultSet rs, String columnName) throws SQLException {
        short result = rs.getShort(columnName);
        return result == 0 && rs.wasNull() ? null : result;
    }

    @Override
    public Short getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        short result = rs.getShort(columnIndex);
        return result == 0 && rs.wasNull() ? null : result;
    }
}