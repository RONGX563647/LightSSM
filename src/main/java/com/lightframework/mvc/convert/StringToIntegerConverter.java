package com.lightframework.mvc.convert;

public class StringToIntegerConverter implements Converter<String, Integer> {
    @Override
    public Integer convert(String source) {
        return Integer.parseInt(source);
    }

    @Override
    public Class<String> getSourceType() {
        return String.class;
    }

    @Override
    public Class<Integer> getTargetType() {
        return Integer.class;
    }
}
