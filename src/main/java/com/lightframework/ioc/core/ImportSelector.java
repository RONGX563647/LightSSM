package com.lightframework.ioc.core;

/**
 * ImportSelector 接口：用于根据条件动态选择要导入的类。
 * 实现该接口的类可以通过 selectImports() 方法返回需要注册的全限定类名数组。
 */
public interface ImportSelector {
    
    /**
     * 选择要导入的类的全限定类名。
     * @return 需要注册的全限定类名数组
     */
    String[] selectImports();
}
