package com.wulicode.weiran.automation.application.task;

import com.wulicode.weiran.automation.api.AutomationErrors;
import com.wulicode.weiran.automation.api.task.AutomationReviewFileView;
import com.wulicode.weiran.automation.api.task.AutomationReviewView;
import com.wulicode.weiran.automation.api.task.AutomationRunDetailView;
import com.wulicode.weiran.automation.api.task.AutomationRunView;
import com.wulicode.weiran.automation.api.task.AutomationTaskService;
import com.wulicode.weiran.automation.api.task.AutomationTaskView;
import com.wulicode.weiran.automation.domain.task.AutomationReview;
import com.wulicode.weiran.automation.domain.task.AutomationReviewFile;
import com.wulicode.weiran.automation.domain.task.AutomationRunRepository;
import com.wulicode.weiran.automation.domain.task.AutomationTask;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRun;
import com.wulicode.weiran.framework.error.BizException;
import com.wulicode.weiran.framework.page.PageQuery;
import com.wulicode.weiran.framework.page.PageResult;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

/** 自动化任务查询与启停用例。 */
public class AutomationTaskApplicationService implements AutomationTaskService {

    /** 业务时区：与数据库 datetime 列的写入口径保持一致。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AutomationTaskRepository tasks;

    private final AutomationRunRepository runs;

    private final AutomationTaskLauncher launcher;

    /** 构造用例服务。 */
    public AutomationTaskApplicationService(
            final AutomationTaskRepository tasks,
            final AutomationRunRepository runs,
            final AutomationTaskLauncher launcher) {
        this.tasks = tasks;
        this.runs = runs;
        this.launcher = launcher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AutomationTaskView> list() {
        final LocalDateTime now = LocalDateTime.now(AutomationTaskApplicationService.ZONE);
        return this.tasks.findAll().stream()
                .map(task -> AutomationTaskApplicationService.toView(task, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AutomationRunView> listRuns(final long taskId, final PageQuery query) {
        this.requireTask(taskId);
        final long total = this.runs.countRuns(taskId);
        final List<AutomationRunView> rows = this.runs.findRuns(taskId, query.offset(), query.pageSize()).stream()
                .map(AutomationTaskApplicationService::toRunView)
                .toList();
        return PageResult.of(rows, total, query);
    }

    @Override
    @Transactional(readOnly = true)
    public AutomationRunDetailView getRun(final long taskId, final long runId) {
        this.requireTask(taskId);
        final AutomationTaskRun run = this.runs
                .findRun(runId)
                .filter(row -> row.taskId() == taskId)
                .orElseThrow(() -> new BizException(AutomationErrors.RUN_NOT_FOUND));
        final List<AutomationReviewView> reviews = this.runs.findReviews(runId).stream()
                .map(AutomationTaskApplicationService::toReviewView)
                .toList();
        return new AutomationRunDetailView(AutomationTaskApplicationService.toRunView(run), reviews);
    }

    @Override
    @Transactional
    public AutomationTaskView setEnabled(final long id, final boolean enabled) {
        this.requireTask(id);
        this.tasks.updateEnabled(id, enabled);
        return AutomationTaskApplicationService.toView(
                this.requireTask(id), LocalDateTime.now(AutomationTaskApplicationService.ZONE));
    }

    /**
     * 立即执行一次。
     *
     * <p>这里不包事务：抢占租约必须先提交，异步线程才能读到；失败时按冲突返回，交给管理员重试。
     */
    @Override
    public AutomationTaskView trigger(final long id) {
        final AutomationTask task = this.requireTask(id);
        final LocalDateTime now = LocalDateTime.now(AutomationTaskApplicationService.ZONE);
        if (task.isRunning(now)) {
            throw BizException.conflict("任务正在执行，请等本轮结束");
        }
        final int interval = Math.max(task.intervalSeconds(), 30);
        if (!this.tasks.claim(id, now, now.plusSeconds(interval), now.plusMinutes(30), false)) {
            throw BizException.conflict("任务刚刚被触发或正在执行");
        }
        this.launcher.submit(task, AutomationTaskLauncher.TRIGGER_MANUAL);
        return AutomationTaskApplicationService.toView(this.requireTask(id), now);
    }

    @Override
    @Transactional
    public void deleteRun(final long taskId, final long runId) {
        this.requireTask(taskId);
        final boolean belongsToTask =
                this.runs.findRun(runId).filter(row -> row.taskId() == taskId).isPresent();
        if (!belongsToTask) {
            throw new BizException(AutomationErrors.RUN_NOT_FOUND);
        }
        this.runs.deleteReviewsByRun(runId);
        this.runs.deleteRun(runId);
        final List<AutomationTaskRun> latest = this.runs.findRuns(taskId, 0L, 1);
        if (latest.isEmpty()) {
            this.tasks.clearLastRun(taskId);
            return;
        }
        final AutomationTaskRun last = latest.get(0);
        this.tasks.recordRunResult(
                taskId,
                last.id(),
                last.status(),
                last.summary(),
                last.finishedAt() == null ? last.startedAt() : last.finishedAt());
    }

    private AutomationTask requireTask(final long id) {
        return this.tasks.findById(id).orElseThrow(() -> new BizException(AutomationErrors.TASK_NOT_FOUND));
    }

    private static AutomationTaskView toView(final AutomationTask task, final LocalDateTime now) {
        return new AutomationTaskView(
                task.id(),
                task.code(),
                task.name(),
                task.description(),
                task.triggerDescription(),
                task.scheduleDescription(),
                task.scheduleKind(),
                task.intervalSeconds(),
                task.outputMode().value(),
                task.enabled(),
                task.lastRunAt(),
                task.lastRunStatus(),
                task.lastRunSummary(),
                task.lastRunId(),
                task.isRunning(now),
                task.createdAt(),
                task.updatedAt());
    }

    private static AutomationRunView toRunView(final AutomationTaskRun run) {
        return new AutomationRunView(
                run.id(),
                run.taskId(),
                run.triggerType(),
                run.outputMode().value(),
                run.status().value(),
                run.startedAt(),
                run.finishedAt(),
                run.durationMs(),
                run.summary(),
                run.errorMessage(),
                run.mergeRequestCount(),
                run.reviewedCount(),
                run.commentCount());
    }

    private static AutomationReviewView toReviewView(final AutomationReview review) {
        return new AutomationReviewView(
                review.id(),
                review.runId(),
                review.projectId(),
                review.mergeRequestId(),
                review.mergeRequestTitle(),
                review.webUrl(),
                review.authorName(),
                review.sourceBranch(),
                review.targetBranch(),
                review.status().value(),
                review.commentText(),
                review.model(),
                review.fileCount(),
                review.addLines(),
                review.delLines(),
                review.errorMessage(),
                review.files().stream()
                        .map(AutomationTaskApplicationService::toFileView)
                        .toList());
    }

    private static AutomationReviewFileView toFileView(final AutomationReviewFile file) {
        return new AutomationReviewFileView(
                file.id(),
                file.path(),
                file.addLines(),
                file.delLines(),
                file.level().value(),
                file.commentText(),
                file.skipReason());
    }
}
