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

    // TODO [L1][练习] 补充 transferTo(java.io.File dest) 便捷方法，将 bytes 写入目标文件，并在 bytes 为空时按 isEmpty() 抛出合理异常；写对标志：实现后运行本类/本包对应单测（无则新建一个），断言目标行为成立且运行期不抛异常；若是框架扩展点，给出容器内可复现的最小示例。
    //   同时校验 dest 父目录存在。验收标准：transferTo 后目标文件内容与上传字节一致。
}
