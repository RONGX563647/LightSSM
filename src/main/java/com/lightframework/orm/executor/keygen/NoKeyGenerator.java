package com.rongx.mybatis.executor.keygen;

import com.rongx.mybatis.executor.Executor;
import com.rongx.mybatis.mapping.MappedStatement;

import java.sql.Statement;


public class NoKeyGenerator implements KeyGenerator {

    @Override
    public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

    @Override
    public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
        // Do Nothing
    }

}
