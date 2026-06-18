package com.lightframework.spi.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lightframework.spi.exception.JsonSerializationException;
import com.lightframework.spi.json.JsonSerializer;

import java.io.IOException;
import java.io.OutputStream;

public class JacksonSerializer implements JsonSerializer {

    private final ObjectMapper mapper;

    public JacksonSerializer() {
        this.mapper = createDefaultMapper();
    }

    public JacksonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    private static final boolean JAVA_TIME_MODULE_AVAILABLE;

    static {
        boolean found = false;
        try {
            Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
            found = true;
        } catch (ClassNotFoundException e) {
            // JavaTimeModule not available
        }
        JAVA_TIME_MODULE_AVAILABLE = found;
    }

    private static ObjectMapper createDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        if (JAVA_TIME_MODULE_AVAILABLE) {
            try {
                Class<?> javaTimeModule = Class.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
                mapper.registerModule((com.fasterxml.jackson.databind.Module) javaTimeModule.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                // Should not happen since we checked in static init
            }
        }
        return mapper;
    }

    @Override
    public String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to serialize: " + obj, e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to deserialize: " + json, e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            JavaType javaType = mapper.getTypeFactory().constructType(typeRef.getType());
            return mapper.readValue(json, javaType);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to deserialize: " + json, e);
        }
    }

    @Override
    public void writeValue(OutputStream out, Object obj) throws IOException {
        mapper.writeValue(out, obj);
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
