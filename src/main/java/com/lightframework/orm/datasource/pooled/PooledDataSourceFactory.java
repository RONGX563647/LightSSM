package com.lightframework.orm.datasource.pooled;

import com.lightframework.orm.datasource.unpooled.UnpooledDataSourceFactory;


public class PooledDataSourceFactory extends UnpooledDataSourceFactory {

    public PooledDataSourceFactory() {
        this.dataSource = new PooledDataSource();
    }

}
