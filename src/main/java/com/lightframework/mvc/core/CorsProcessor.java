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
        if (origins.contains("*")) return true;
        for (String allowed : origins) {
            if (allowed.equals(origin)) return true;
            if (allowed.endsWith("*") && origin.startsWith(allowed.substring(0, allowed.length() - 1))) {
                return true;
            }
        }
        return false;
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
}
