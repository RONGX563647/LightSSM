package com.rongx.mybatis.type;

import java.sql.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * LocalTime类型处理器 (Java 8+)
 */
public class LocalTimeTypeHandler extends BaseTypeHandler<LocalTime> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, LocalTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setTime(i, Time.valueOf(parameter));
    }

    @Override
    protected LocalTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Time time = rs.getTime(columnName);
        return time != null ? time.toLocalTime() : null;
    }

    @Override
    public LocalTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Time time = rs.getTime(columnIndex);
        return time != null ? time.toLocalTime() : null;
    }
}