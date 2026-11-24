package com.wulicode.weiran.automation.api.task;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 一次自动化任务执行的结果摘要。 */
public record AutomationRunView(
        long id,
        long taskId,
        String triggerType,
        String outputMode,
        String status,
        LocalDateTime startedAt,
        @Nullable LocalDateTime finishedAt,
        @Nullable Long durationMs,
        String summary,
        @Nullable String errorMessage,
        int mergeRequestCount,
        int reviewedCount,
        int commentCount) {}
