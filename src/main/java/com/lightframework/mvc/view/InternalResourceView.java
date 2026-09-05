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
            // TODO [L1][练习] 当前把 model 的每个 entry 直接 setAttribute 到 request；请明确"已存在同名 request attribute 时的策略"；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
            //   （覆盖 or 跳过），并补充单元测试验证。验收标准：行为与你的约定一致，不意外丢失既有 attribute。
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