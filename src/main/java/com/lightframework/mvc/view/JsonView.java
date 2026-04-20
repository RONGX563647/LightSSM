package com.lightframework.mvc.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Map;

public class JsonView implements View {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void render(Map<String, Object> model, HttpServletRequest request, 
        HttpServletResponse response) throws Exception {
        
        response.setContentType("application/json;charset=UTF-8");
        
        Object data = model;
        if (model != null && model.containsKey("jsonData")) {
            data = model.get("jsonData");
        }
        
        String json = objectMapper.writeValueAsString(data);
        
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}