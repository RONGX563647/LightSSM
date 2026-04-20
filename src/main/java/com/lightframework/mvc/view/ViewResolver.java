package com.lightframework.mvc.view;

public interface ViewResolver {
    View resolveViewName(String viewName) throws Exception;
}