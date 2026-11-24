package com.wulicode.weiran.automation.domain.task;

import org.jspecify.annotations.Nullable;

/** 评审 Agent 对单个文件的结论。 */
public record ReviewFileDraft(
        String path,
        ReviewFileLevel level,
        @Nullable String commentText,
        @Nullable String skipReason) {}
