package com.lightframework.spi.json;

import com.lightframework.spi.Ordered;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public interface JsonSerializer extends Ordered {

    String toJson(Object obj);

    <T> T fromJson(String json, Class<T> type);

    <T> T fromJson(String json, TypeReference<T> typeRef);

    default void writeValue(OutputStream out, Object obj) throws IOException {
        out.write(toJson(obj).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    default int getOrder() { return Ordered.LOWEST_PRECEDENCE; }

    class TypeReference<T> {
        private final java.lang.reflect.Type type;

        protected TypeReference() {
            java.lang.reflect.Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof java.lang.reflect.ParameterizedType) {
                this.type = ((java.lang.reflect.ParameterizedType) superclass).getActualTypeArguments()[0];
            } else {
                throw new RuntimeException("TypeReference must be parameterized");
            }
        }

        public java.lang.reflect.Type getType() { return type; }
    }
}
