package com.lightframework.mvc.multipart;

import jakarta.servlet.http.HttpServletRequest;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public class MultipartResolver {

    private static final String MULTIPART_FORM_DATA = "multipart/form-data";
    private long maxFileSize = 10 * 1024 * 1024;

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MULTIPART_FORM_DATA);
    }

    public Map<String, List<MultipartFile>> resolveMultipart(HttpServletRequest request) throws Exception {
        Map<String, List<MultipartFile>> files = new LinkedHashMap<>();

        String contentType = request.getContentType();
        String boundary = extractBoundary(contentType);
        if (boundary == null) return files;

        InputStream is = request.getInputStream();
        byte[] body = readStream(is);

        List<byte[]> parts = splitParts(body, boundary);
        for (byte[] part : parts) {
            parsePart(part, files);
        }

        return files;
    }

    private String extractBoundary(String contentType) {
        int idx = contentType.indexOf("boundary=");
        if (idx == -1) return null;
        String boundary = contentType.substring(idx + 9).trim();
        if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
            boundary = boundary.substring(1, boundary.length() - 1);
        }
        return boundary;
    }

    private byte[] readStream(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }

    private List<byte[]> splitParts(byte[] body, String boundary) {
        List<byte[]> parts = new ArrayList<>();
        byte[] boundaryBytes = ("--" + boundary).getBytes();
        byte[] endBoundaryBytes = ("--" + boundary + "--").getBytes();

        int start = indexOf(body, boundaryBytes, 0);
        if (start == -1) return parts;
        start += boundaryBytes.length;

        while (true) {
            int end = indexOf(body, boundaryBytes, start);
            if (end == -1) break;

            int partLen = end - start;
            if (partLen > 2 && body[end - 2] == '\r' && body[end - 1] == '\n') {
                partLen -= 2;
            }

            byte[] part = new byte[partLen];
            System.arraycopy(body, start, part, 0, partLen);
            parts.add(part);
            start = end + boundaryBytes.length;
        }

        int lastBoundary = indexOf(body, endBoundaryBytes, 0);
        if (lastBoundary != -1) {
            int finalEnd = indexOf(body, boundaryBytes, lastBoundary + endBoundaryBytes.length);
            if (finalEnd != -1) {
                int partLen = finalEnd - (lastBoundary + endBoundaryBytes.length);
                if (partLen > 2) {
                    if (body[lastBoundary + endBoundaryBytes.length] == '\r'
                        && body[lastBoundary + endBoundaryBytes.length + 1] == '\n') {
                        byte[] part = new byte[partLen - 2];
                        System.arraycopy(body, lastBoundary + endBoundaryBytes.length + 2, part, 0, partLen - 2);
                        parts.add(part);
                    }
                }
            }
        }

        return parts;
    }

    private int indexOf(byte[] data, byte[] pattern, int start) {
        outer:
        for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private void parsePart(byte[] part, Map<String, List<MultipartFile>> files) throws Exception {
        String partStr = new String(part, "ISO-8859-1");
        int headerEnd = partStr.indexOf("\r\n\r\n");
        if (headerEnd == -1) return;

        String headerSection = partStr.substring(0, headerEnd);
        int contentStart = headerEnd + 4;

        String name = extractHeaderValue(headerSection, "name");
        String filename = extractHeaderValue(headerSection, "filename");
        String contentType = extractContentType(headerSection);

        int bodyLength = part.length - contentStart;
        if (bodyLength > maxFileSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
        byte[] fileContent = new byte[bodyLength];
        System.arraycopy(part, contentStart, fileContent, 0, bodyLength);

        if (filename != null && !filename.isEmpty()) {
            MultipartFile multipartFile = new MultipartFile(name, filename, contentType, fileContent);
            files.computeIfAbsent(name, k -> new ArrayList<>()).add(multipartFile);
        }
    }

    private String extractHeaderValue(String headerSection, String paramName) {
        int idx = headerSection.indexOf(paramName + "=\"");
        if (idx == -1) return null;
        int start = idx + paramName.length() + 2;
        int end = headerSection.indexOf("\"", start);
        return end != -1 ? headerSection.substring(start, end) : null;
    }

    private String extractContentType(String headerSection) {
        int idx = headerSection.toLowerCase().indexOf("content-type:");
        if (idx == -1) return null;
        int start = headerSection.indexOf(":", idx) + 1;
        while (start < headerSection.length() && headerSection.charAt(start) == ' ') start++;
        int end = headerSection.indexOf("\r\n", start);
        return end != -1 ? headerSection.substring(start, end).trim() : headerSection.substring(start).trim();
    }
}
