package com.lightframework.orm.type;

import java.sql.*;

/**
 * 字节对象数组类型处理器 (Byte[])
 */
public class ByteObjectArrayTypeHandler extends BaseTypeHandler<Byte[]> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Byte[] parameter, JdbcType jdbcType) throws SQLException {
        byte[] bytes = new byte[parameter.length];
        for (int j = 0; j < parameter.length; j++) {
            bytes[j] = parameter[j];
        }
        ps.setBytes(i, bytes);
    }

    @Override
    protected Byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return toByteObjectArray(bytes);
    }

    @Override
    public Byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return toByteObjectArray(bytes);
    }

    private Byte[] toByteObjectArray(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        Byte[] result = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }
}