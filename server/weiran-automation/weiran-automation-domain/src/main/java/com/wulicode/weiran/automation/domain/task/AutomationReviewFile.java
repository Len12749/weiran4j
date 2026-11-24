package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/** 单个变更文件的评审意见。 */
public record AutomationReviewFile(
        long id,
        long reviewId,
        String path,
        int addLines,
        int delLines,
        ReviewFileLevel level,
        @Nullable String commentText,
        @Nullable String skipReason) {}
