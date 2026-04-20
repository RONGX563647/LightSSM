package com.rongx.mybatis.type;

import java.sql.*;

/**
 * Byte类型处理器
 */
public class ByteTypeHandler extends BaseTypeHandler<Byte> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Byte parameter, JdbcType jdbcType) throws SQLException {
        ps.setByte(i, parameter);
    }

    @Override
    protected Byte getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte result = rs.getByte(columnName);
        return result == 0 && rs.wasNull() ? null : result;
    }

    @Override
    public Byte getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte result = rs.getByte(columnIndex);
        return result == 0 && rs.wasNull() ? null : result;
    }
}