package com.rongx.mybatis.reflection.wrapper;

import com.rongx.mybatis.reflection.MetaObject;


public interface ObjectWrapperFactory {

    /**
     * 判断有没有包装器
     */
    boolean hasWrapperFor(Object object);

    /**
     * 得到包装器
     */
    ObjectWrapper getWrapperFor(MetaObject metaObject, Object object);

}
