package com.lightframework.mvc.test;

import com.lightframework.mvc.core.ModelAndView;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModelAndViewTest {

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（addObject / addAllObjects 合并）。
    @Test
    void addObjectAndGetModel() {
        ModelAndView mv = new ModelAndView("view");
        mv.addObject("k", "v");
        assertEquals("view", mv.getViewName());
        assertEquals("v", mv.getModel().get("k"));
    }

    @Test
    void addAllObjectsMerges() {
        ModelAndView mv = new ModelAndView("view");
        mv.addAllObjects(Map.of("a", 1, "b", 2));
        assertEquals(1, mv.getModel().get("a"));
        assertEquals(2, mv.getModel().get("b"));
    }

    // 验证 TODO[L1] 练习目标：用户手写实现后运行本测试应全绿（isReference 识别 redirect:）。
    @Test
    void isReferenceRecognizesRedirect() {
        assertTrue(new ModelAndView("redirect:/x").isReference());
        assertFalse(new ModelAndView("home").isReference());
    }

    @Test
    void hasViewAndEmpty() {
        assertTrue(new ModelAndView("v").hasView());
        assertTrue(new ModelAndView().isEmpty());
    }

    @Test
    void clearResetsState() {
        ModelAndView mv = new ModelAndView("v");
        mv.addObject("k", "v");
        mv.clear();
        assertTrue(mv.isEmpty());
    }
}
