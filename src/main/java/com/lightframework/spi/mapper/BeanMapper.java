package com.lightframework.spi.mapper;

import com.lightframework.spi.Ordered;
import java.util.List;

public interface BeanMapper extends Ordered {
    <T> T map(Object source, Class<T> targetType);

    void copyProperties(Object source, Object target);

    <T> List<T> mapList(List<?> sources, Class<T> targetType);

    @Override
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
