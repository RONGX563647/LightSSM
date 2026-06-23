package com.lightframework.mvc.multipart;

import java.io.InputStream;

public class MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] bytes;

    public MultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.bytes = bytes;
    }

    public String getName() {
        return name;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getSize() {
        return bytes != null ? bytes.length : 0;
    }

    public boolean isEmpty() {
        return bytes == null || bytes.length == 0;
    }

    public InputStream getInputStream() {
        return new java.io.ByteArrayInputStream(bytes);
    }
}
