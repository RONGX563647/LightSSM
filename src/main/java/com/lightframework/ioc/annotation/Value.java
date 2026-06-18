package com.lightframework.ioc.annotation;

import java.lang.annotation.*;

/**
 * Annotation for injecting values from property placeholders.
 * Compatible with Spring's org.springframework.beans.factory.annotation.Value
 *
 * <p>Supports ${key} placeholder syntax and ${key:default} default value syntax.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@literal @}Value("${app.name}")
 * private String appName;
 *
 * {@literal @}Value("${app.port:8080}")
 * private int port;
 * </pre>
 * @deprecated 移到了 {@link com.lightframework.di.annotation.Value}
 */
@Deprecated
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    /**
     * The value expression to resolve, typically in the form "${property.key}"
     * or "${property.key:defaultValue}".
     *
     * @return the value expression
     */
    String value() default "";
}
