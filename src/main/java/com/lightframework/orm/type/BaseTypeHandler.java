package com.rongx.mybatis.type;

import com.rongx.mybatis.session.Configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseTypeHandler<T> implements TypeHandler<T> {

    protected Configuration configuration;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            if (jdbcType == null) {
                // 如果没有指定jdbcType，则根据数据库驱动设置NULL
                ps.setNull(i, java.sql.Types.NULL);
            } else {
                // 使用指定的jdbcType设置NULL
                ps.setNull(i, jdbcType.TYPE_CODE);
            }
        } else {
            // 调用子类实现设置非空参数
            setNonNullParameter(ps, i, parameter, jdbcType);
        }
    }

    @Override
    public T getResult(ResultSet rs, String columnName) throws SQLException {
        try {
            return getNullableResult(rs, columnName);
        } catch (SQLException e) {
            // 增强错误信息
            throw new SQLException("Error getting result for column: " + columnName, e);
        }
    }

    @Override
    public T getResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            return getNullableResult(rs, columnIndex);
        } catch (SQLException e) {
            // 增强错误信息
            throw new SQLException("Error getting result for column index: " + columnIndex, e);
        }
    }

    protected abstract void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException;

    protected abstract T getNullableResult(ResultSet rs, String columnName) throws SQLException;

    public abstract T getNullableResult(ResultSet rs, int columnIndex) throws SQLException;
}