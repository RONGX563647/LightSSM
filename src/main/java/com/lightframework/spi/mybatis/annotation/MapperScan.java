package com.lightframework.spi.mybatis.annotation;

import com.lightframework.ioc.annotation.Import;
import com.lightframework.spi.mybatis.autoconfigure.MapperScanRegistrar;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MapperScanRegistrar.class)
public @interface MapperScan {
    String[] value() default {};

    String basePackage() default "";
}
