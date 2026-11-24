package com.wulicode.weiran.automation.domain.task;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 一个合并请求的评审结果（按合并请求聚合，含逐文件意见）。 */
public record AutomationReview(
        long id,
        long runId,
        long taskId,
        String projectId,
        String repositoryPath,
        long mergeRequestId,
        String mergeRequestTitle,
        @Nullable String webUrl,
        @Nullable String authorName,
        @Nullable String sourceBranch,
        @Nullable String targetBranch,
        AutomationReviewStatus status,
        @Nullable String commentText,
        @Nullable String model,
        int fileCount,
        int addLines,
        int delLines,
        @Nullable String errorMessage,
        List<AutomationReviewFile> files) {

    /** 防御性复制，避免调用方持有的可变列表影响结果。 */
    public AutomationReview {
        files = List.copyOf(files);
    }
}
