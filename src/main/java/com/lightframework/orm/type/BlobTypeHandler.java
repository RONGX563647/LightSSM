package com.lightframework.orm.type;

import java.sql.*;

/**
 * Blob类型处理器
 */
public class BlobTypeHandler extends BaseTypeHandler<Blob> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Blob parameter, JdbcType jdbcType) throws SQLException {
        ps.setBlob(i, parameter);
    }

    @Override
    protected Blob getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBlob(columnName);
    }

    @Override
    public Blob getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBlob(columnIndex);
    }
}