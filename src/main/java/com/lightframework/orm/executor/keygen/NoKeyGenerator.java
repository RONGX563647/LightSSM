package com.lightframework.orm.executor.keygen;

import com.lightframework.orm.executor.Executor;
import com.lightframework.orm.mapping.MappedStatement;

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
