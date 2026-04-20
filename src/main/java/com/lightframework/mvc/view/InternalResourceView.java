package com.lightframework.mvc.view;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public class InternalResourceView implements View {
    
    private String url;
    
    public InternalResourceView(String url) {
        this.url = url;
    }
    
    @Override
    public void render(Map<String, Object> model, HttpServletRequest request, 
        HttpServletResponse response) throws Exception {
        
        if (model != null) {
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        request.getRequestDispatcher(this.url).forward(request, response);
    }
    
    public String getUrl() {
        return this.url;
    }
}