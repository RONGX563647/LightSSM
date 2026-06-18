package com.lightframework.orm.type;

import java.sql.*;
import java.time.LocalDate;

/**
 * LocalDate类型处理器 (Java 8+)
 */
public class LocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setDate(i, Date.valueOf(parameter));
        } else {
            ps.setNull(i, Types.DATE);
        }
    }

    @Override
    protected LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Date date = rs.getDate(columnName);
        return date != null ? date.toLocalDate() : null;
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Date date = rs.getDate(columnIndex);
        return date != null ? date.toLocalDate() : null;
    }
}