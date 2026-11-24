package com.wulicode.weiran.automation.domain.task;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 一次任务执行的不可变记录；{@code id} 为 0 表示尚未入库。 */
public record AutomationTaskRun(
        long id,
        long taskId,
        String triggerType,
        OutputMode outputMode,
        AutomationRunStatus status,
        LocalDateTime startedAt,
        @Nullable LocalDateTime finishedAt,
        @Nullable Long durationMs,
        String summary,
        @Nullable String errorMessage,
        int mergeRequestCount,
        int reviewedCount,
        int commentCount) {

    /** 计划中的执行（尚未结束）。 */
    public static AutomationTaskRun started(
            final long taskId, final String triggerType, final OutputMode outputMode, final LocalDateTime startedAt) {
        return new AutomationTaskRun(
                0L,
                taskId,
                triggerType,
                outputMode,
                AutomationRunStatus.FAILED,
                startedAt,
                null,
                null,
                "",
                null,
                0,
                0,
                0);
    }
}
