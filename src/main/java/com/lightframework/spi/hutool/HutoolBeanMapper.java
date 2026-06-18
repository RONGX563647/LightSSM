package com.lightframework.spi.hutool;

import com.lightframework.spi.Ordered;
import com.lightframework.spi.mapper.BeanMapper;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.ReflectUtil;

import java.util.List;
import java.util.stream.Collectors;

public class HutoolBeanMapper implements BeanMapper {

    @Override
    public <T> T map(Object source, Class<T> targetType) {
        if (source == null) return null;
        T target = ReflectUtil.newInstance(targetType);
        BeanUtil.copyProperties(source, target, CopyOptions.create()
            .setIgnoreNullValue(true)
            .setIgnoreError(true));
        return target;
    }

    @Override
    public void copyProperties(Object source, Object target) {
        BeanUtil.copyProperties(source, target, CopyOptions.create()
            .setIgnoreNullValue(true)
            .setIgnoreError(true));
    }

    @Override
    public <T> List<T> mapList(List<?> sources, Class<T> targetType) {
        return sources.stream()
            .map(s -> map(s, targetType))
            .collect(Collectors.toList());
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
