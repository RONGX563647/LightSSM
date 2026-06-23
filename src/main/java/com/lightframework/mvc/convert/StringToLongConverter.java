package com.lightframework.mvc.convert;

public class StringToLongConverter implements Converter<String, Long> {
    @Override
    public Long convert(String source) {
        return Long.parseLong(source);
    }

    @Override
    public Class<String> getSourceType() {
        return String.class;
    }

    @Override
    public Class<Long> getTargetType() {
        return Long.class;
    }
}
