package com.lightframework.mvc.view;

public class InternalResourceViewResolver implements ViewResolver {
    
    private String prefix = "/WEB-INF/views/";
    private String suffix = ".jsp";
    
    public InternalResourceViewResolver() {
    }
    
    public InternalResourceViewResolver(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }
    
    @Override
    public View resolveViewName(String viewName) throws Exception {
        if (viewName == null) {
            return null;
        }
        
        if (viewName.startsWith("redirect:")) {
            String redirectUrl = viewName.substring("redirect:".length());
            return new RedirectView(redirectUrl);
        }
        
        if (viewName.startsWith("forward:")) {
            String forwardUrl = viewName.substring("forward:".length());
            return new InternalResourceView(forwardUrl);
        }
        
        String url = this.prefix + viewName + this.suffix;
        return new InternalResourceView(url);
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}