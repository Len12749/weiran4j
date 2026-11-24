package com.wulicode.weiran.automation.domain.task;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 自动化任务定义。
 *
 * <p>任务由迁移固定播种，界面只能启停；{@code code} 是执行器选择处理逻辑的唯一键。
 */
public record AutomationTask(
        long id,
        String code,
        String name,
        String description,
        String triggerDescription,
        String scheduleDescription,
        String scheduleKind,
        int intervalSeconds,
        OutputMode outputMode,
        boolean enabled,
        @Nullable LocalDateTime lastRunAt,
        @Nullable String lastRunStatus,
        @Nullable String lastRunSummary,
        @Nullable Long lastRunId,
        @Nullable LocalDateTime nextRunAt,
        @Nullable LocalDateTime lockedUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** 云效待评审合并请求质检任务的固定编码。 */
    public static final String CODE_YUNXIAO_PR_REVIEW = "yunxiao-pr-review";

    /** 是否仍被某个实例持有执行租约。 */
    public boolean isRunning(final LocalDateTime now) {
        return this.lockedUntil != null && this.lockedUntil.isAfter(now);
    }
}
