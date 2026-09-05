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
        
        // TODO [L1][练习] 当前重定向 URL 未保留原始查询串、也未对非 ASCII 做 URL 编码；请补充：当 redirectUrl 不含查询串时拼接原请求的 queryString，；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并对路径/参数做 URLEncoder.encode（UTF-8）。验收标准：重定向到含中文参数的地址时浏览器能正确解析。
        response.sendRedirect(targetUrl);
    }
    
    public String getRedirectUrl() {
        return this.redirectUrl;
    }
}