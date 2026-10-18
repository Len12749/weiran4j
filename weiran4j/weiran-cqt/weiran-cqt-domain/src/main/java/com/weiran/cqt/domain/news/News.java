package com.weiran.cqt.domain.news;

import java.time.LocalDateTime;
import org.jspecify.annotations.NullMarked;

/** 大赛动态。 */
@NullMarked
public record News(
        long id,
        String title,
        String image,
        boolean showOnHome,
        int sort,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
