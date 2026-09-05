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
        
        // TODO [L2][练习] 当前仅按 redirect:/forward: 前缀与默认拼接来选 View；请结合 ContentNegotiationManager 的结果选择 View；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   （如协商为 application/json 时返回 JsonView，而非默认 JSP）。验收标准：方法返回 "user" 且协商为 JSON 时得到 JsonView。
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