package com.lightframework.mvc.core;

import com.lightframework.mvc.annotation.CrossOrigin;
import com.lightframework.mvc.handler.HandlerMethod;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CorsProcessor {

    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>();
    private List<String> allowedHeaders = new ArrayList<>();
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public CorsProcessor() {
        allowedOrigins.add("*");
        allowedMethods.add("GET");
        allowedMethods.add("POST");
        allowedMethods.add("PUT");
        allowedMethods.add("DELETE");
        allowedMethods.add("PATCH");
        allowedMethods.add("OPTIONS");
        allowedHeaders.add("*");
    }

    public boolean processRequest(HttpServletRequest request, HttpServletResponse response) {
        return processRequest(request, response, null);
    }

    public boolean processRequest(HttpServletRequest request, HttpServletResponse response,
        Object handler) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            return true;
        }

        CrossOriginConfig config = resolveCrossOrigin(handler);

        List<String> origins = config.origins;
        List<String> methods = config.methods;
        List<String> headers = config.headers;

        if (!isOriginAllowed(origin, origins)) {
            return true;
        }

        String httpMethod = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(httpMethod)) {
            // TODO [L3][练习] 预检请求当前只设置了允许的 Methods/Headers，但未校验本次请求的 Method 是否真的在 config.methods 列表中；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
            //   请在返回 200 前增加"请求方法不在允许列表则拒绝（不写 Allow-Methods 或返回 403）"的校验。
            //   验收标准：前端用未被允许的 DELETE 做预检时，浏览器拿不到对应的 Access-Control-Allow-Methods。
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", String.join(", ", methods));
            response.setHeader("Access-Control-Allow-Headers", String.join(", ", headers));
            if (config.maxAge > 0) {
                response.setHeader("Access-Control-Max-Age", String.valueOf(config.maxAge));
            }
            if (config.allowCredentials) {
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }

        response.setHeader("Access-Control-Allow-Origin", origin);
        if (config.allowCredentials) {
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        if (!config.exposedHeaders.isEmpty()) {
            response.setHeader("Access-Control-Expose-Headers", String.join(", ", config.exposedHeaders));
        }
        return true;
    }

    private CrossOriginConfig resolveCrossOrigin(Object handler) {
        if (handler instanceof HandlerMethod hm) {
            Method method = hm.getMethod();
            CrossOrigin methodAnn = method.getAnnotation(CrossOrigin.class);
            if (methodAnn != null) {
                return fromAnnotation(methodAnn);
            }
            CrossOrigin classAnn = hm.getBeanType().getAnnotation(CrossOrigin.class);
            if (classAnn != null) {
                return fromAnnotation(classAnn);
            }
        }
        return new CrossOriginConfig(allowedOrigins, allowedMethods, allowedHeaders,
            List.of(), allowCredentials, maxAge);
    }

    private CrossOriginConfig fromAnnotation(CrossOrigin ann) {
        List<String> origins = ann.origins().length > 0
            ? Arrays.asList(ann.origins()) : allowedOrigins;
        List<String> methods = ann.methods().length > 0
            ? Arrays.asList(ann.methods()) : allowedMethods;
        List<String> headers = ann.allowedHeaders().length > 0
            ? Arrays.asList(ann.allowedHeaders()) : allowedHeaders;
        List<String> exposed = ann.exposedHeaders().length > 0
            ? Arrays.asList(ann.exposedHeaders()) : List.of();
        long age = ann.maxAge() > 0 ? ann.maxAge() : maxAge;
        return new CrossOriginConfig(origins, methods, headers, exposed,
            ann.allowCredentials(), age);
    }

    private boolean isOriginAllowed(String origin, List<String> origins) {
        // TODO [L3][优化-策略模式] origin 是否放行的判定（精确匹配、"*"通配、后缀通配 "*.example.com"）目前写死在 if 里；写对标志：按策略模式完成实现，新增单测覆盖“运行时切换不同策略得到不同结果”的主路径与一条未知策略的异常路径。
        //   可把不同匹配规则抽象为 OriginMatcher 策略，通过工厂组合成匹配链，新增规则（如正则、子域）零改动接入。
        if (origins.contains("*")) return true;
        for (String allowed : origins) {
            if (allowed.equals(origin)) return true;
            // 后缀通配（如 *.example.com）：去掉前导 "*." 后，按"源的 host 以该域名结尾"匹配
            if (allowed.startsWith("*.")) {
                String domain = allowed.substring(2);
                String host = extractHost(origin);
                if (host != null && (host.equals(domain) || host.endsWith("." + domain))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String extractHost(String origin) {
        if (origin == null) return null;
        int schemeEnd = origin.indexOf("://");
        String rest = schemeEnd >= 0 ? origin.substring(schemeEnd + 3) : origin;
        int slash = rest.indexOf('/');
        if (slash >= 0) rest = rest.substring(0, slash);
        int colon = rest.indexOf(':');
        if (colon >= 0) rest = rest.substring(0, colon);
        return rest.isEmpty() ? null : rest;
    }

    private record CrossOriginConfig(
        List<String> origins,
        List<String> methods,
        List<String> headers,
        List<String> exposedHeaders,
        boolean allowCredentials,
        long maxAge
    ) {}

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    // TODO [L1][练习] 把 isOriginAllowed 中的"后缀通配匹配"逻辑（allowed.startsWith("*.") && host.endsWith(...)）抽取为；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   独立可单测的 matchSuffixWildcard(origin, allowed) 方法，并补充单元测试覆盖 "*.foo.com" 与 "api.foo.com" 的命中/不命中。
}
