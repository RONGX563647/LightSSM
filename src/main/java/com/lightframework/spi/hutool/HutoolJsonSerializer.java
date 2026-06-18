package com.lightframework.spi.hutool;

import com.lightframework.spi.exception.JsonSerializationException;
import com.lightframework.spi.json.JsonSerializer;
import cn.hutool.json.JSONUtil;

public class HutoolJsonSerializer implements JsonSerializer {

    @Override
    public String toJson(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to serialize: " + obj, e);
        }
    }

    @Override
    public <T> T fromJson(String json, Class<T> type) {
        try {
            return JSONUtil.toBean(json, type);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to deserialize: " + json, e);
        }
    }

    @Override
    public <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return JSONUtil.toBean(json, typeRef.getType(), false);
        } catch (Exception e) {
            throw new JsonSerializationException("Failed to deserialize: " + json, e);
        }
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
