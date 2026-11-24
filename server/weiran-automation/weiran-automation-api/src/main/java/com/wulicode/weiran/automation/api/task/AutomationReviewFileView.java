package com.wulicode.weiran.automation.api.task;

import org.jspecify.annotations.Nullable;

/** 单个变更文件的评审意见。 */
public record AutomationReviewFileView(
        long id,
        String path,
        int addLines,
        int delLines,
        String level,
        @Nullable String commentText,
        @Nullable String skipReason) {}
