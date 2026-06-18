package com.lightframework.orm.type;

import java.math.BigInteger;
import java.sql.*;

/**
 * BigInteger类型处理器
 */
public class BigIntegerTypeHandler extends BaseTypeHandler<BigInteger> {

    @Override
    protected void setNonNullParameter(PreparedStatement ps, int i, BigInteger parameter, JdbcType jdbcType) throws SQLException {
        if (parameter != null) {
            ps.setString(i, parameter.toString());
        } else {
            ps.setNull(i, Types.VARCHAR);
        }
    }

    @Override
    protected BigInteger getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? new BigInteger(value) : null;
    }

    @Override
    public BigInteger getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? new BigInteger(value) : null;
    }
}