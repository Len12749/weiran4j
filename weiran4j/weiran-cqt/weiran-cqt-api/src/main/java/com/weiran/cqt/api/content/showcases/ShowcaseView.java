package com.weiran.cqt.api.content.showcases;
/** 佳作欣赏数据。 */
public record ShowcaseView(
        long id,
        String title,
        String image,
        String category,
        String author,
        String teacher,
        String school,
        String description,
        String content,
        String attachmentUrl,
        String attachmentName,
        String videoUrl,
        boolean enabled,
        int sort,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
