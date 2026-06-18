package com.lightframework.spi.mybatis.autoconfigure;

public class MyBatisProperties {
    private String mapperBasePackage = "com.app.dao";
    private String configLocation;
    private boolean camelCase = true;

    public String getMapperBasePackage() { return mapperBasePackage; }
    public void setMapperBasePackage(String mapperBasePackage) { this.mapperBasePackage = mapperBasePackage; }
    public String getConfigLocation() { return configLocation; }
    public void setConfigLocation(String configLocation) { this.configLocation = configLocation; }
    public boolean isCamelCase() { return camelCase; }
    public void setCamelCase(boolean camelCase) { this.camelCase = camelCase; }
}
