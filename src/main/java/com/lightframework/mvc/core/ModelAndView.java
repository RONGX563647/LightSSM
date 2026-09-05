package com.lightframework.mvc.core;

import java.util.Map;

public class ModelAndView {
    private String viewName;
    private Map<String, Object> model;
    private boolean cleared = false;
    
    public ModelAndView() {
    }
    
    public ModelAndView(String viewName) {
        this.viewName = viewName;
    }
    
    public ModelAndView(String viewName, Map<String, Object> model) {
        this.viewName = viewName;
        this.model = model;
    }
    
    public ModelAndView addObject(String attributeName, Object attributeValue) {
        if (this.model == null) {
            this.model = new java.util.HashMap<>();
        }
        this.model.put(attributeName, attributeValue);
        return this;
    }
    
    public ModelAndView addAllObjects(Map<String, ?> modelMap) {
        if (this.model == null) {
            this.model = new java.util.HashMap<>();
        }
        // TODO [L1][练习] addAllObjects 直接 putAll 会覆盖同名 key；请改为"遇到已存在的 key 时合并/忽略"的可配置策略，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并补充单元测试验证覆盖与忽略两种行为。验收标准：与调用方约定一致，不产生意外的 key 丢失。
        this.model.putAll(modelMap);
        return this;
    }
    
    public String getViewName() {
        return this.viewName;
    }
    
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }
    
    public Map<String, Object> getModel() {
        return this.model;
    }
    
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }
    
    public boolean hasView() {
        return this.viewName != null;
    }
    
    public boolean isReference() {
        // TODO [L1][练习] 当前 isReference() 只识别 "redirect:" 前缀；请同时识别 "forward:" 前缀（forward 视图也是"引用"而非模板名），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并提取为可配置的 REFERENCE_PREFIXES 集合。验收标准：isReference() 对 "forward:/x" 也返回 true。
        return this.viewName != null && this.viewName.startsWith("redirect:");
    }
    
    public void clear() {
        this.viewName = null;
        this.model = null;
        this.cleared = true;
    }
    
    public boolean isEmpty() {
        return this.viewName == null && (this.model == null || this.model.isEmpty());
    }
}