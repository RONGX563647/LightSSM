package com.lightframework.mvc.test;

import com.lightframework.mvc.annotation.CookieValue;
import com.lightframework.mvc.annotation.PathVariable;
import com.lightframework.mvc.annotation.RequestBody;
import com.lightframework.mvc.annotation.RequestHeader;
import com.lightframework.mvc.annotation.RequestMapping;
import com.lightframework.mvc.annotation.RequestParam;
import com.lightframework.mvc.annotation.ResponseBody;
import com.lightframework.mvc.core.ModelAndView;
import com.lightframework.mvc.handler.HandlerMethod;
import com.lightframework.mvc.handler.RequestMappingHandlerAdapter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RequestMappingHandlerAdapterTest {

    static class User {
        public String name;
        public int age;

        public User() {
        }

        public User(String name) {
            this.name = name;
        }
    }

    static class Ctrl {
        @RequestMapping("/echo")
        public String echo(@RequestParam("name") String name) {
            return "Echo:" + name;
        }

        @RequestMapping("/num")
        public String num(@RequestParam("n") int n) {
            return "n" + n;
        }

        @RequestMapping("/u")
        public String user(@PathVariable("id") Long id) {
            return "u" + id;
        }

        @RequestMapping("/hdr")
        public String hdr(@RequestHeader("X-Token") String token) {
            return "tok:" + token;
        }

        @RequestMapping("/ck")
        public String ck(@CookieValue("sid") String sid) {
            return "sid:" + sid;
        }

        @ResponseBody
        @RequestMapping("/body")
        public String body(@RequestBody User u) {
            return u.name;
        }

        @ResponseBody
        @RequestMapping("/get")
        public User get() {
            return new User("Alice");
        }
    }

    private final RequestMappingHandlerAdapter adapter = new RequestMappingHandlerAdapter();
    private final Ctrl ctrl = new Ctrl();

    private HandlerMethod hm(String methodName, Class<?>... paramTypes) throws Exception {
        return new HandlerMethod(ctrl, Ctrl.class.getDeclaredMethod(methodName, paramTypes));
    }

    private static ServletInputStream servletInputStream(byte[] data) {
        return new ServletInputStream() {
            private int pos = 0;

            @Override
            public int read() {
                return pos < data.length ? data[pos++] & 0xFF : -1;
            }

            @Override
            public boolean isFinished() {
                return pos >= data.length;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {
            }
        };
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（@RequestParam 绑定与基本类型转换）。
    @Test
    void bindsRequestParamAndConvertsInt() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameter("name")).thenReturn("abc");
        when(request.getParameter("n")).thenReturn("7");

        ModelAndView mv = adapter.handle(request, mock(HttpServletResponse.class), hm("echo", String.class));
        assertEquals("Echo:abc", mv.getViewName());

        ModelAndView mv2 = adapter.handle(request, mock(HttpServletResponse.class), hm("num", int.class));
        assertEquals("n7", mv2.getViewName());
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（@PathVariable 从 request 属性绑定）。
    @Test
    void bindsPathVariable() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(
                com.lightframework.mvc.handler.RequestMappingInfo.PATH_VARIABLES_ATTRIBUTE))
                .thenReturn(java.util.Map.of("id", "42"));

        ModelAndView mv = adapter.handle(request, mock(HttpServletResponse.class), hm("user", Long.class));
        assertEquals("u42", mv.getViewName());
    }

    @Test
    void bindsRequestHeader() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Token")).thenReturn("tok");

        ModelAndView mv = adapter.handle(request, mock(HttpServletResponse.class), hm("hdr", String.class));
        assertEquals("tok:tok", mv.getViewName());
    }

    @Test
    void bindsCookieValue() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("sid", "abc")});

        ModelAndView mv = adapter.handle(request, mock(HttpServletResponse.class), hm("ck", String.class));
        assertEquals("sid:abc", mv.getViewName());
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（@RequestBody JSON 反序列化参数）。
    @Test
    void bindsRequestBodyJson() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        byte[] json = "{\"name\":\"Bob\"}".getBytes(StandardCharsets.UTF_8);
        when(request.getInputStream()).thenReturn(servletInputStream(json));

        StringWriter sw = new StringWriter();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        adapter.handle(request, response, hm("body", User.class));
        assertTrue(sw.toString().contains("Bob"));
    }

    // 验证 TODO[L2] 练习目标：用户手写实现后运行本测试应全绿（@ResponseBody 把返回值序列化为 JSON）。
    @Test
    void responseBodySerializesObject() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        StringWriter sw = new StringWriter();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        adapter.handle(request, response, hm("get"));
        assertTrue(sw.toString().contains("Alice"));
    }
}
