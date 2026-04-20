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