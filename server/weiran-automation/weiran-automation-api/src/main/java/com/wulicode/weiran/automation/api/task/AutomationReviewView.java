package com.wulicode.weiran.automation.api.task;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 一个合并请求的评审结果，含逐文件意见。 */
public record AutomationReviewView(
        long id,
        long runId,
        String projectId,
        long mergeRequestId,
        String mergeRequestTitle,
        @Nullable String webUrl,
        @Nullable String authorName,
        @Nullable String sourceBranch,
        @Nullable String targetBranch,
        String status,
        @Nullable String commentText,
        @Nullable String model,
        int fileCount,
        int addLines,
        int delLines,
        @Nullable String errorMessage,
        List<AutomationReviewFileView> files) {}
