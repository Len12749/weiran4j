package com.wulicode.weiran.automation.application.task;

import com.wulicode.weiran.automation.domain.task.AutomationReadiness;
import com.wulicode.weiran.automation.domain.task.AutomationReview;
import com.wulicode.weiran.automation.domain.task.AutomationReviewFile;
import com.wulicode.weiran.automation.domain.task.AutomationReviewStatus;
import com.wulicode.weiran.automation.domain.task.AutomationRunRepository;
import com.wulicode.weiran.automation.domain.task.AutomationRunStatus;
import com.wulicode.weiran.automation.domain.task.AutomationTask;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRun;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSource;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSummary;
import com.wulicode.weiran.automation.domain.task.ChangedFile;
import com.wulicode.weiran.automation.domain.task.CodeRepositoryPort;
import com.wulicode.weiran.automation.domain.task.CodeRepositoryPort.RepositorySyncException;
import com.wulicode.weiran.automation.domain.task.OutputMode;
import com.wulicode.weiran.automation.domain.task.ReviewAgent;
import com.wulicode.weiran.automation.domain.task.ReviewDraft;
import com.wulicode.weiran.automation.domain.task.ReviewFileLevel;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单个自动化任务的执行编排。
 *
 * <p>顺序固定为：输出方式与配置检查 → 拉取待评审合并请求 → 过滤本地已评审 → 同步代码库 → 变更分析 → 落库。
 * 任何一步失败都会留下可读的执行结果，不会静默跳过。
 */
public class AutomationTaskExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomationTaskExecutor.class);

    /** 单轮最多处理的合并请求数，避免一次执行长时间占用调度线程。 */
    private static final int MAX_MERGE_REQUESTS_PER_RUN = 3;

    /** 摘要列长度上限。 */
    private static final int SUMMARY_MAX_LENGTH = 500;

    /** 失败原因列长度上限。 */
    private static final int ERROR_MAX_LENGTH = 900;

    /** 业务时区：与数据库 datetime 列的写入口径保持一致。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AutomationTaskRepository tasks;

    private final AutomationRunRepository runs;

    private final ChangeRequestSource source;

    private final CodeRepositoryPort repositories;

    private final ReviewAgent agent;

    private final AutomationReadiness readiness;

    private final ExecutorService reviewExecutor;

    /** 构造执行器。 */
    public AutomationTaskExecutor(
            final AutomationTaskRepository tasks,
            final AutomationRunRepository runs,
            final ChangeRequestSource source,
            final CodeRepositoryPort repositories,
            final ReviewAgent agent,
            final AutomationReadiness readiness,
            final ExecutorService reviewExecutor) {
        this.tasks = tasks;
        this.runs = runs;
        this.source = source;
        this.repositories = repositories;
        this.agent = agent;
        this.readiness = readiness;
        this.reviewExecutor = reviewExecutor;
    }

    /** 执行一轮任务；异常在内部收敛为失败记录。 */
    public void execute(final AutomationTask task, final String triggerType) {
        final LocalDateTime startedAt = LocalDateTime.now(AutomationTaskExecutor.ZONE);
        final OutputMode outputMode = task.outputMode();
        if (!AutomationTask.CODE_YUNXIAO_PR_REVIEW.equals(task.code())) {
            this.recordFailure(
                    task, triggerType, startedAt, outputMode, "暂不支持的任务类型：" + task.code(), "unsupported task code");
            return;
        }
        if (outputMode == OutputMode.LOCAL_AND_CLOUD) {
            this.recordFailure(
                    task,
                    triggerType,
                    startedAt,
                    outputMode,
                    "云端回填尚未实现，任务已自动停用；请等待回填能力上线后再开启",
                    "cloud write-back is not implemented");
            this.tasks.updateEnabled(task.id(), false);
            return;
        }
        final List<String> missing = this.readiness.missingItems();
        if (!missing.isEmpty()) {
            final String reason = String.join("；", missing);
            this.recordFailure(
                    task, triggerType, startedAt, outputMode, "执行前检查未通过：" + reason + "。任务已自动停用，补齐配置后重新开启", reason);
            this.tasks.updateEnabled(task.id(), false);
            LOGGER.warn("Automation task {} disabled because of missing configuration: {}", task.code(), reason);
            return;
        }
        final List<ChangeRequestSummary> pending;
        try {
            pending = this.source.listPendingReviews();
        } catch (final RuntimeException ex) {
            final String reason = AutomationTaskExecutor.brief(ex);
            this.recordFailure(task, triggerType, startedAt, outputMode, "拉取云效待评审合并请求失败：" + reason, reason);
            LOGGER.warn("Automation task {} failed to list change requests: {}", task.code(), reason);
            return;
        }
        final List<ChangeRequestSummary> todo = pending.stream()
                .filter(request -> !this.runs.existsReviewed(task.id(), request.projectId(), request.localId()))
                .toList();
        if (pending.isEmpty() || todo.isEmpty()) {
            // 没有新增内容：不产生执行记录，只更新任务的最近检查结果，避免每分钟堆积空记录。
            final String summary =
                    pending.isEmpty() ? "本轮没有待评审的合并请求" : "本轮 " + pending.size() + " 个待评审合并请求均已在本地生成过评论，未重复执行";
            this.tasks.recordCheckResult(
                    task.id(), AutomationRunStatus.SUCCESS, summary, LocalDateTime.now(AutomationTaskExecutor.ZONE));
            return;
        }
        final long runId =
                this.runs.createRun(AutomationTaskRun.started(task.id(), triggerType, outputMode, startedAt));
        final List<ChangeRequestSummary> batch =
                todo.subList(0, Math.min(AutomationTaskExecutor.MAX_MERGE_REQUESTS_PER_RUN, todo.size()));
        // 同一轮里多个合并请求并行分析：每个合并请求先抢占本地评审权，抢不到的直接跳过，
        // 因此上一轮还在处理的合并请求不会被这一轮重复分析（唯一键兜底，跨实例同样成立）。
        final List<CompletableFuture<ReviewOutcome>> futures = batch.stream()
                .map(request ->
                        CompletableFuture.supplyAsync(() -> this.reviewOne(task, runId, request), this.reviewExecutor))
                .toList();
        final List<ReviewOutcome> outcomes =
                futures.stream().map(CompletableFuture::join).toList();
        final int reviewed = (int) outcomes.stream()
                .filter(outcome -> outcome == ReviewOutcome.REVIEWED)
                .count();
        final int failed = (int) outcomes.stream()
                .filter(outcome -> outcome == ReviewOutcome.FAILED)
                .count();
        final int skipped = (int) outcomes.stream()
                .filter(outcome -> outcome == ReviewOutcome.SKIPPED)
                .count();
        final AutomationRunStatus status;
        if (failed == 0) {
            status = AutomationRunStatus.SUCCESS;
        } else if (reviewed > 0) {
            status = AutomationRunStatus.PARTIAL;
        } else {
            status = AutomationRunStatus.FAILED;
        }
        final StringBuilder summary = new StringBuilder()
                .append("待评审 ")
                .append(pending.size())
                .append(" 个，本轮处理 ")
                .append(batch.size())
                .append(" 个：生成评论 ")
                .append(reviewed)
                .append(" 条");
        if (failed > 0) {
            summary.append("，失败 ").append(failed).append(" 个");
        }
        if (skipped > 0) {
            summary.append("，跳过 ").append(skipped).append(" 个");
        }
        final int remaining = todo.size() - batch.size();
        if (remaining > 0) {
            summary.append("；剩余 ").append(remaining).append(" 个将在下一轮处理");
        }
        this.finish(
                runId,
                task.id(),
                startedAt,
                status,
                summary.toString(),
                failed == 0 ? null : "存在失败的合并请求，详见评论明细",
                pending.size(),
                reviewed,
                reviewed);
    }

    /** 处理单个合并请求：抢占评审权 → 同步代码库 → 拉变更 → 评审 → 落库。 */
    private ReviewOutcome reviewOne(final AutomationTask task, final long runId, final ChangeRequestSummary request) {
        final Optional<Long> claimed =
                this.runs.tryStartReview(task.id(), runId, request, LocalDateTime.now(AutomationTaskExecutor.ZONE));
        if (claimed.isEmpty()) {
            return ReviewOutcome.SKIPPED;
        }
        final long reviewId = claimed.get();
        try {
            final Path repository = this.repositories.synchronize(request.projectId(), request.repositoryPath());
            final List<ChangedFile> files = this.source.loadChangedFiles(request);
            final List<ChangedFile> analyzable =
                    files.stream().filter(AutomationTaskExecutor::isAnalyzable).toList();
            if (analyzable.isEmpty()) {
                this.runs.completeReview(reviewId, AutomationTaskExecutor.skippedReview(request, "没有可分析的代码变更"));
                return ReviewOutcome.SKIPPED;
            }
            final ReviewDraft draft = this.agent.review(request, analyzable, repository);
            this.runs.completeReview(reviewId, AutomationTaskExecutor.reviewedReview(request, draft, analyzable));
            return ReviewOutcome.REVIEWED;
        } catch (final RuntimeException ex) {
            final String reason = ex instanceof RepositorySyncException
                    ? String.valueOf(ex.getMessage())
                    : AutomationTaskExecutor.brief(ex);
            this.runs.completeReview(reviewId, AutomationTaskExecutor.failedReview(request, reason));
            LOGGER.warn(
                    "Automation task {} failed to review {}/{}: {}",
                    task.code(),
                    request.projectId(),
                    request.localId(),
                    reason);
            return ReviewOutcome.FAILED;
        }
    }

    /** 单个合并请求的处理结果。 */
    private enum ReviewOutcome {
        REVIEWED,
        FAILED,
        SKIPPED
    }

    /** 记录一次不产生评审结果的失败执行。 */
    private void recordFailure(
            final AutomationTask task,
            final String triggerType,
            final LocalDateTime startedAt,
            final OutputMode outputMode,
            final String summary,
            final String error) {
        final long runId =
                this.runs.createRun(AutomationTaskRun.started(task.id(), triggerType, outputMode, startedAt));
        this.finish(runId, task.id(), startedAt, AutomationRunStatus.FAILED, summary, error, 0, 0, 0);
    }

    private void finish(
            final long runId,
            final long taskId,
            final LocalDateTime startedAt,
            final AutomationRunStatus status,
            final String summary,
            final @Nullable String error,
            final int pending,
            final int reviewed,
            final int comments) {
        final LocalDateTime finishedAt = LocalDateTime.now(AutomationTaskExecutor.ZONE);
        final long duration = Duration.between(startedAt, finishedAt).toMillis();
        final String clipped = AutomationTaskExecutor.clip(summary, AutomationTaskExecutor.SUMMARY_MAX_LENGTH);
        this.runs.finishRun(
                runId,
                status,
                finishedAt,
                duration,
                clipped,
                error == null ? null : AutomationTaskExecutor.clip(error, AutomationTaskExecutor.ERROR_MAX_LENGTH),
                pending,
                reviewed,
                comments);
        this.tasks.recordRunResult(taskId, runId, status, clipped, finishedAt);
    }

    /** 只保留值得送进模型的代码类变更。 */
    private static boolean isAnalyzable(final ChangedFile file) {
        if (file.binary() || file.deleted() || file.diff().isBlank()) {
            return false;
        }
        final String path = file.path().toLowerCase(Locale.ROOT);
        if (path.contains("/node_modules/") || path.contains("/dist/") || path.contains("/build/")) {
            return false;
        }
        return path.endsWith(".java")
                || path.endsWith(".kt")
                || path.endsWith(".ts")
                || path.endsWith(".tsx")
                || path.endsWith(".js")
                || path.endsWith(".jsx")
                || path.endsWith(".vue")
                || path.endsWith(".php")
                || path.endsWith(".go")
                || path.endsWith(".py")
                || path.endsWith(".sql")
                || path.endsWith(".yml")
                || path.endsWith(".yaml")
                || path.endsWith(".xml")
                || path.endsWith(".properties");
    }

    private static AutomationReview skippedReview(final ChangeRequestSummary request, final String reason) {
        return new AutomationReview(
                0L,
                0L,
                0L,
                request.projectId(),
                request.localId(),
                request.title(),
                request.webUrl(),
                request.authorName(),
                request.sourceBranch(),
                request.targetBranch(),
                AutomationReviewStatus.SKIPPED,
                null,
                null,
                0,
                0,
                0,
                null,
                List.of(new AutomationReviewFile(0L, 0L, "-", 0, 0, ReviewFileLevel.SKIP, null, reason)));
    }

    private static AutomationReview reviewedReview(
            final ChangeRequestSummary request, final ReviewDraft draft, final List<ChangedFile> analyzable) {
        final int addLines = analyzable.stream().mapToInt(ChangedFile::addLines).sum();
        final int delLines = analyzable.stream().mapToInt(ChangedFile::delLines).sum();
        final List<AutomationReviewFile> files = draft.files().stream()
                .map(file -> new AutomationReviewFile(
                        0L,
                        0L,
                        file.path(),
                        AutomationTaskExecutor.fileLines(analyzable, file.path(), true),
                        AutomationTaskExecutor.fileLines(analyzable, file.path(), false),
                        file.level(),
                        file.commentText(),
                        file.skipReason()))
                .toList();
        return new AutomationReview(
                0L,
                0L,
                0L,
                request.projectId(),
                request.localId(),
                request.title(),
                request.webUrl(),
                request.authorName(),
                request.sourceBranch(),
                request.targetBranch(),
                AutomationReviewStatus.REVIEWED,
                draft.commentText(),
                draft.model(),
                analyzable.size(),
                addLines,
                delLines,
                null,
                files);
    }

    private static AutomationReview failedReview(final ChangeRequestSummary request, final @Nullable String reason) {
        return new AutomationReview(
                0L,
                0L,
                0L,
                request.projectId(),
                request.localId(),
                request.title(),
                request.webUrl(),
                request.authorName(),
                request.sourceBranch(),
                request.targetBranch(),
                AutomationReviewStatus.FAILED,
                null,
                null,
                0,
                0,
                0,
                AutomationTaskExecutor.clip(reason, AutomationTaskExecutor.ERROR_MAX_LENGTH),
                List.of());
    }

    private static int fileLines(final List<ChangedFile> files, final String path, final boolean added) {
        return files.stream()
                .filter(file -> file.path().equals(path))
                .findFirst()
                .map(file -> added ? file.addLines() : file.delLines())
                .orElse(0);
    }

    private static String brief(final RuntimeException ex) {
        final String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            return ex.getClass().getSimpleName();
        }
        return AutomationTaskExecutor.clip(message, AutomationTaskExecutor.ERROR_MAX_LENGTH);
    }

    private static String clip(final @Nullable String value, final int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
