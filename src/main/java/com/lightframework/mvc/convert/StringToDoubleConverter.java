package com.lightframework.mvc.convert;

public class StringToDoubleConverter implements Converter<String, Double> {
    @Override
    public Double convert(String source) {
        return Double.parseDouble(source);
    }

    @Override
    public Class<String> getSourceType() {
        return String.class;
    }

    @Override
    public Class<Double> getTargetType() {
        return Double.class;
    }
}
