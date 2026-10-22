package com.weiran.cqt.domain.content.resources;
/** 指导资料数据。 */
public record Resource(
        long id,
        String title,
        String category,
        String materialUrl,
        String content,
        boolean enabled,
        int sort,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
