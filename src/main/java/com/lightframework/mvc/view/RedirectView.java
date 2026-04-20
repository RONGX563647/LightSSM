package com.lightframework.mvc.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public class RedirectView implements View {
    
    private String redirectUrl;
    
    public RedirectView(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
    
    @Override
    public void render(Map<String, Object> model, HttpServletRequest request, 
        HttpServletResponse response) throws Exception {
        
        String targetUrl = this.redirectUrl;
        if (targetUrl.startsWith("/")) {
            targetUrl = request.getContextPath() + targetUrl;
        }
        
        response.sendRedirect(targetUrl);
    }
    
    public String getRedirectUrl() {
        return this.redirectUrl;
    }
}