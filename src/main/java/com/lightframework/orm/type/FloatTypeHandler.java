package com.lightframework.orm.type;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Float类型处理器
 */
public class FloatTypeHandler extends BaseTypeHandler<Float> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, Float parameter, JdbcType jdbcType) throws SQLException {
        ps.setFloat(i, parameter);
    }

    @Override
    protected Float getNullableResult(ResultSet rs, String columnName) throws SQLException {
        float result = rs.getFloat(columnName);
        return result == 0 && rs.wasNull() ? null : result;
    }

    @Override
    public Float getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        float result = rs.getFloat(columnIndex);
        return result == 0 && rs.wasNull() ? null : result;
    }
}