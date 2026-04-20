package com.rongx.mybatis.type;

import java.sql.*;

/**
 * 字节数组类型处理器
 */
public class ByteArrayTypeHandler extends BaseTypeHandler<byte[]> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType) throws SQLException {
        ps.setBytes(i, parameter);
    }

    @Override
    protected byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBytes(columnName);
    }

    @Override
    public byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }
}