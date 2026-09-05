package com.lightframework.mvc.test;

import com.lightframework.mvc.handler.RequestMappingInfo;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestMappingInfoTest {

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（路径变量解析）。
    @Test
    void exactPathMatches() {
        RequestMappingInfo info = new RequestMappingInfo("/hello", "GET");
        Map<String, String> vars = info.match("/hello", "GET");
        assertNotNull(vars);
        assertTrue(vars.isEmpty());
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（{var} 变量提取）。
    @Test
    void pathVariableMatches() {
        RequestMappingInfo info = new RequestMappingInfo("/user/{id}", "GET");
        Map<String, String> vars = info.match("/user/42", "GET");
        assertNotNull(vars);
        assertEquals("42", vars.get("id"));
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（HTTP 方法不匹配应返回 null）。
    @Test
    void methodMismatchReturnsNull() {
        RequestMappingInfo info = new RequestMappingInfo("/hello", "POST");
        assertNull(info.match("/hello", "GET"));
    }

    @Test
    void noMatchReturnsNull() {
        RequestMappingInfo info = new RequestMappingInfo("/hello", "GET");
        assertNull(info.match("/other", "GET"));
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（不带方法约束的映射匹配任意方法）。
    @Test
    void emptyMethodMatchesAny() {
        RequestMappingInfo info = new RequestMappingInfo("/hello", "");
        assertNotNull(info.match("/hello", "DELETE"));
    }
}
