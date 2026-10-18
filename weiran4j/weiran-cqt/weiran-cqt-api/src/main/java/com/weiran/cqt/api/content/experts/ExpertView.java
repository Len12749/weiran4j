package com.weiran.cqt.api.content.experts;
/** 学术评审团数据。 */
public record ExpertView(
        long id,
        String name,
        String image,
        String content,
        boolean showOnHome,
        boolean enabled,
        int sort,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
