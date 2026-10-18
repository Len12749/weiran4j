package com.weiran.cqt.api.news;

import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;

/** 大赛动态管理视图。 */
@NullMarked
public record NewsView(
        long id,
        String title,
        String image,
        boolean showOnHome,
        int sort,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
