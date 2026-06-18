package com.lightframework.orm.type;

import java.sql.*;

/**
 * java.sql.Timestamp类型处理器
 */
public class SqlTimestampTypeHandler extends BaseTypeHandler<Timestamp> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Timestamp parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, parameter);
    }

    @Override
    protected Timestamp getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getTimestamp(columnName);
    }

    @Override
    public Timestamp getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }
}