package com.rongx.mybatis.datasource.pooled;

import com.rongx.mybatis.datasource.unpooled.UnpooledDataSourceFactory;


public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

    public PooledDataSourceFactory() {
        this.dataSource = new PooledDataSource();
    }

}
