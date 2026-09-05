package com.lightframework.mvc.core;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

public class ContentNegotiationManager {

    private final List<String> mediaTypes = new ArrayList<>();

    public ContentNegotiationManager() {
        mediaTypes.add("application/json");
        mediaTypes.add("text/html");
        mediaTypes.add("text/plain");
        mediaTypes.add("application/xml");
    }

    public String resolveMediaType(HttpServletRequest request) {
        // TODO [L2][练习] 当前协商只看 Accept 头与 format 参数；请补充"按请求路径后缀协商"的能力（如 /a.json -> application/json、/a.xml -> application/xml），；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
        //   并在后缀、参数、Accept 头之间存在冲突时定义清楚的优先级。验收标准：请求 /user.json 时返回 application/json，忽略 Accept: text/html。
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader == null) {
            String format = request.getParameter("format");
            if (format != null) {
                return formatToMediaType(format);
            }
            return "application/json";
        }

        for (String mediaType : mediaTypes) {
            if (acceptHeader.contains(mediaType)) {
                return mediaType;
            }
        }

        String firstType = acceptHeader.split(",")[0].trim();
        if (!firstType.equals("*/*")) {
            return firstType;
        }

        return "application/json";
    }

    private String formatToMediaType(String format) {
        // TODO [L3][优化-策略模式] 当前用 switch 把 format 映射到 mediaType；可把每种映射抽象为 MediaTypeStrategy（如 JsonStrategy、XmlStrategy），；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
        //   注册到 Map<String, MediaTypeStrategy> 并由工厂构建，消除 switch 分支、支持热插拔新格式。
        return switch (format.toLowerCase()) {
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html" -> "text/html";
            case "text" -> "text/plain";
            default -> "application/json";
        };
    }

    public void addMediaType(String mediaType) {
        this.mediaTypes.add(mediaType);
    }

    public boolean isJsonRequest(HttpServletRequest request) {
        String mediaType = resolveMediaType(request);
        return mediaType.equals("application/json")
            || mediaType.contains("+json");
    }
}
