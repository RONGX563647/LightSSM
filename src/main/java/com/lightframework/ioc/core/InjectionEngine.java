package com.lightframework.ioc.core;

import com.lightframework.di.core.DependencyContainer;
import com.lightframework.di.core.PlaceholderResolver;

/**
 * @deprecated 移到了 com.lightframework.di.core.InjectionEngine
 */
@Deprecated
public class InjectionEngine extends com.lightframework.di.core.InjectionEngine {
    public InjectionEngine(DependencyContainer container,
                           DefaultTypeConverter typeConverter,
                           PlaceholderResolver placeholderResolver) {
        super(container, typeConverter, placeholderResolver);
    }
}
