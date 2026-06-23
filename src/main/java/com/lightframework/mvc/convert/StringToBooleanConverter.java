package com.lightframework.mvc.convert;

public class StringToBooleanConverter implements Converter<String, Boolean> {
    @Override
    public Boolean convert(String source) {
        return Boolean.parseBoolean(source);
    }

    @Override
    public Class<String> getSourceType() {
        return String.class;
    }

    @Override
    public Class<Boolean> getTargetType() {
        return Boolean.class;
    }
}
