package com.lightframework.mvc.test;

import com.lightframework.mvc.servlet.DispatcherServlet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DispatcherServletTest {

    // 验证 TODO[L3] 练习目标：用户手写实现后运行本测试应全绿。
    // 这里记录 Web 作用域的集成点：在 DispatcherServlet.doDispatch 中，请求进入时应调用
    // RequestScope.setCurrentRequest(request) 把当前 request 绑定到 Web 作用域，处理结束（finally）时清理，
    // 使 @RequestScope / @SessionScope Bean 在 Controller 中可用（当前仓库尚未实现该绑定，故此处只验证 DispatcherServlet 可构造）。
    @Test
    void dispatcherServletIsConstructable() {
        assertNotNull(new DispatcherServlet());
    }
}
