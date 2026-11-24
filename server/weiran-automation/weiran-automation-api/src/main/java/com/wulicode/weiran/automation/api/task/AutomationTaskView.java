package com.wulicode.weiran.automation.api.task;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 自动化任务视图。
 *
 * <p>{@code running} 由执行租约推导：租约未过期表示当前有实例正在执行该任务。
 */
public record AutomationTaskView(
        long id,
        String code,
        String name,
        String description,
        String triggerDescription,
        String scheduleDescription,
        String scheduleKind,
        int intervalSeconds,
        String outputMode,
        boolean enabled,
        @Nullable LocalDateTime lastRunAt,
        @Nullable String lastRunStatus,
        @Nullable String lastRunSummary,
        @Nullable Long lastRunId,
        boolean running,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
