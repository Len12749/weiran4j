package com.wulicode.weiran.automation.domain.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 自动化任务领域模型与枚举解析规则。 */
class AutomationDomainTest {

    @Test
    @DisplayName("输出方式：未知值按仅本地处理，避免误回写云端")
    void outputModeFallsBackToLocal() {
        assertThat(OutputMode.of("local_and_cloud")).isEqualTo(OutputMode.LOCAL_AND_CLOUD);
        assertThat(OutputMode.of("local")).isEqualTo(OutputMode.LOCAL);
        assertThat(OutputMode.of("cloud")).isEqualTo(OutputMode.LOCAL);
        assertThat(OutputMode.of(null)).isEqualTo(OutputMode.LOCAL);
    }

    @Test
    @DisplayName("执行状态：只接受已知取值")
    void runStatusParsesKnownValues() {
        assertThat(AutomationRunStatus.of("success")).contains(AutomationRunStatus.SUCCESS);
        assertThat(AutomationRunStatus.of("partial")).contains(AutomationRunStatus.PARTIAL);
        assertThat(AutomationRunStatus.of("failed")).contains(AutomationRunStatus.FAILED);
        assertThat(AutomationRunStatus.of("running")).contains(AutomationRunStatus.RUNNING);
        assertThat(AutomationRunStatus.of("unknown")).isEmpty();
        assertThat(AutomationRunStatus.SUCCESS.value()).isEqualTo("success");
    }

    @Test
    @DisplayName("评审状态：未知值按失败处理，避免脏数据被当成成功")
    void reviewStatusFallsBackToFailed() {
        assertThat(AutomationReviewStatus.of("reviewed")).isEqualTo(AutomationReviewStatus.REVIEWED);
        assertThat(AutomationReviewStatus.of("processing")).isEqualTo(AutomationReviewStatus.PROCESSING);
        assertThat(AutomationReviewStatus.of("skipped")).isEqualTo(AutomationReviewStatus.SKIPPED);
        assertThat(AutomationReviewStatus.of("weird")).isEqualTo(AutomationReviewStatus.FAILED);
    }

    @Test
    @DisplayName("文件结论：大小写不敏感，未知值按改进建议处理")
    void reviewFileLevelNormalizes() {
        assertThat(ReviewFileLevel.of("ISSUE")).isEqualTo(ReviewFileLevel.ISSUE);
        assertThat(ReviewFileLevel.of(" pass ")).isEqualTo(ReviewFileLevel.PASS);
        assertThat(ReviewFileLevel.of("skip")).isEqualTo(ReviewFileLevel.SKIP);
        assertThat(ReviewFileLevel.of("unknown")).isEqualTo(ReviewFileLevel.SUGGEST);
        assertThat(ReviewFileLevel.of(null)).isEqualTo(ReviewFileLevel.SUGGEST);
    }

    @Test
    @DisplayName("执行租约：到期后不再视为运行中")
    void taskRunningDependsOnLease() {
        final LocalDateTime now = LocalDateTime.of(2026, 11, 24, 14, 0);
        final AutomationTask running = task(now.plusMinutes(5));
        final AutomationTask expired = task(now.minusMinutes(1));
        final AutomationTask idle = task(null);
        assertThat(running.isRunning(now)).isTrue();
        assertThat(expired.isRunning(now)).isFalse();
        assertThat(idle.isRunning(now)).isFalse();
    }

    @Test
    @DisplayName("新执行记录以未完成状态入库，id 为 0")
    void startedRunIsPlaceholder() {
        final LocalDateTime startedAt = LocalDateTime.of(2026, 11, 24, 14, 0);
        final AutomationTaskRun run = AutomationTaskRun.started(7L, "schedule", OutputMode.LOCAL, startedAt);
        assertThat(run.id()).isZero();
        assertThat(run.taskId()).isEqualTo(7L);
        assertThat(run.outputMode()).isEqualTo(OutputMode.LOCAL);
        assertThat(run.startedAt()).isEqualTo(startedAt);
    }

    @Test
    @DisplayName("评审草稿对文件列表做防御性复制")
    void reviewDraftCopiesFiles() {
        final List<ReviewFileDraft> files = new java.util.ArrayList<>();
        files.add(new ReviewFileDraft("a.java", ReviewFileLevel.PASS, null, null));
        final ReviewDraft draft = new ReviewDraft("ok", "model", files);
        files.clear();
        assertThat(draft.files()).hasSize(1);
    }

    private static AutomationTask task(final @Nullable LocalDateTime lockedUntil) {
        final LocalDateTime createdAt = LocalDateTime.of(2026, 11, 24, 13, 0);
        return new AutomationTask(
                1L,
                AutomationTask.CODE_YUNXIAO_PR_REVIEW,
                "云效 PR 智能质检",
                "说明",
                "触发逻辑",
                "每 1 分钟",
                "interval",
                60,
                OutputMode.LOCAL,
                true,
                null,
                null,
                null,
                null,
                null,
                lockedUntil,
                createdAt,
                createdAt);
    }
}
