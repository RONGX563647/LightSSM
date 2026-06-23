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
