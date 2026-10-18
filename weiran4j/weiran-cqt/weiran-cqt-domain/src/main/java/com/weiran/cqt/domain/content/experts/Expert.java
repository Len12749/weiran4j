package com.weiran.cqt.domain.content.experts;
/** 学术评审团数据。 */
public record Expert(
        long id,
        String name,
        String image,
        String content,
        boolean showOnHome,
        boolean enabled,
        int sort,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt) {}
