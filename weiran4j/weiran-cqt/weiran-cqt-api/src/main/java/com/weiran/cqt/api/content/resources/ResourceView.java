package com.weiran.cqt.api.content.resources;
/** 指导资料数据。 */
public record ResourceView(
        long id,
        String title,
        String category,
        String fileUrl,
        String fileName,
        String content,
        boolean enabled,
        int sort,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
