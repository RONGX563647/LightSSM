package com.lightframework.orm.type;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Instant类型处理器 (Java 8+)
 */
public class InstantTypeHandler extends BaseTypeHandler<Instant> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Instant parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, Timestamp.from(parameter));
    }

    @Override
    protected Instant getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? timestamp.toInstant() : null;
    }

    @Override
    public Instant getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp != null ? timestamp.toInstant() : null;
    }
}