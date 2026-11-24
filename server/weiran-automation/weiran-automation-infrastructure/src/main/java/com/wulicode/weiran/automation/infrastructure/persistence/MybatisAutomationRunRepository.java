package com.wulicode.weiran.automation.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wulicode.weiran.automation.domain.task.AutomationReview;
import com.wulicode.weiran.automation.domain.task.AutomationReviewFile;
import com.wulicode.weiran.automation.domain.task.AutomationReviewStatus;
import com.wulicode.weiran.automation.domain.task.AutomationRunRepository;
import com.wulicode.weiran.automation.domain.task.AutomationRunStatus;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRun;
import com.wulicode.weiran.automation.domain.task.ChangeRequestSummary;
import com.wulicode.weiran.automation.domain.task.OutputMode;
import com.wulicode.weiran.automation.domain.task.ReviewFileLevel;
import com.wulicode.weiran.automation.infrastructure.persistence.entity.AutomationReviewDO;
import com.wulicode.weiran.automation.infrastructure.persistence.entity.AutomationReviewFileDO;
import com.wulicode.weiran.automation.infrastructure.persistence.entity.AutomationTaskRunDO;
import com.wulicode.weiran.automation.infrastructure.persistence.mapper.AutomationReviewFileMapper;
import com.wulicode.weiran.automation.infrastructure.persistence.mapper.AutomationReviewMapper;
import com.wulicode.weiran.automation.infrastructure.persistence.mapper.AutomationTaskRunMapper;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;

/** {@link AutomationRunRepository} 的 MyBatis-Plus 实现。 */
public class MybatisAutomationRunRepository implements AutomationRunRepository {

    private static final String REVIEWED = AutomationReviewStatus.REVIEWED.value();

    /** 处理中的评审超过这个时长视为进程中断，允许重新抢占。 */
    private static final long STALE_MINUTES = 5L;

    /** 业务时区：与数据库 datetime 列的写入口径保持一致。 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final AutomationTaskRunMapper runMapper;

    private final AutomationReviewMapper reviewMapper;

    private final AutomationReviewFileMapper fileMapper;

    /** 构造执行记录仓储。 */
    public MybatisAutomationRunRepository(
            final AutomationTaskRunMapper runMapper,
            final AutomationReviewMapper reviewMapper,
            final AutomationReviewFileMapper fileMapper) {
        this.runMapper = runMapper;
        this.reviewMapper = reviewMapper;
        this.fileMapper = fileMapper;
    }

    @Override
    public long createRun(final AutomationTaskRun run) {
        final AutomationTaskRunDO row = new AutomationTaskRunDO();
        row.setTaskId(run.taskId());
        row.setTriggerType(run.triggerType());
        row.setOutputMode(run.outputMode().value());
        row.setStatus(run.status().value());
        row.setStartedAt(run.startedAt());
        row.setSummary(run.summary() == null || run.summary().isBlank() ? "执行中" : run.summary());
        row.setMergeRequestCount(0);
        row.setReviewedCount(0);
        row.setCommentCount(0);
        this.runMapper.insert(row);
        return row.getId() == null ? 0L : row.getId();
    }

    @Override
    public void finishRun(
            final long id,
            final AutomationRunStatus status,
            final LocalDateTime finishedAt,
            final long durationMs,
            final String summary,
            final @Nullable String errorMessage,
            final int mergeRequestCount,
            final int reviewedCount,
            final int commentCount) {
        this.runMapper.update(
                null,
                Wrappers.<AutomationTaskRunDO>lambdaUpdate()
                        .set(AutomationTaskRunDO::getStatus, status.value())
                        .set(AutomationTaskRunDO::getFinishedAt, finishedAt)
                        .set(AutomationTaskRunDO::getDurationMs, durationMs)
                        .set(AutomationTaskRunDO::getSummary, summary)
                        .set(AutomationTaskRunDO::getErrorMessage, errorMessage)
                        .set(AutomationTaskRunDO::getMergeRequestCount, mergeRequestCount)
                        .set(AutomationTaskRunDO::getReviewedCount, reviewedCount)
                        .set(AutomationTaskRunDO::getCommentCount, commentCount)
                        .eq(AutomationTaskRunDO::getId, id));
    }

    @Override
    public List<AutomationTaskRun> findRuns(final long taskId, final long offset, final int limit) {
        return this.runMapper
                .selectList(Wrappers.lambdaQuery(AutomationTaskRunDO.class)
                        .eq(AutomationTaskRunDO::getTaskId, taskId)
                        .orderByDesc(AutomationTaskRunDO::getStartedAt)
                        .orderByDesc(AutomationTaskRunDO::getId)
                        .last("limit " + limit + " offset " + offset))
                .stream()
                .map(MybatisAutomationRunRepository::toDomain)
                .toList();
    }

    @Override
    public long countRuns(final long taskId) {
        return this.runMapper.selectCount(
                Wrappers.lambdaQuery(AutomationTaskRunDO.class).eq(AutomationTaskRunDO::getTaskId, taskId));
    }

    @Override
    public Optional<AutomationTaskRun> findRun(final long id) {
        return Optional.ofNullable(this.runMapper.selectById(id)).map(MybatisAutomationRunRepository::toDomain);
    }

    @Override
    public List<AutomationReview> findReviews(final long runId) {
        final List<AutomationReviewDO> rows =
                this.reviewMapper.selectList(Wrappers.lambdaQuery(AutomationReviewDO.class)
                        .eq(AutomationReviewDO::getRunId, runId)
                        .orderByAsc(AutomationReviewDO::getId));
        if (rows.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = rows.stream().map(AutomationReviewDO::getId).toList();
        final Map<Long, List<AutomationReviewFileDO>> filesByReview = this.fileMapper
                .selectList(Wrappers.lambdaQuery(AutomationReviewFileDO.class)
                        .in(AutomationReviewFileDO::getReviewId, ids)
                        .orderByAsc(AutomationReviewFileDO::getId))
                .stream()
                .collect(Collectors.groupingBy(AutomationReviewFileDO::getReviewId));
        return rows.stream()
                .map(row -> MybatisAutomationRunRepository.toReview(
                        row, filesByReview.getOrDefault(row.getId(), List.of())))
                .toList();
    }

    @Override
    public boolean existsReviewed(final long taskId, final String projectId, final long mergeRequestId) {
        final long count = this.reviewMapper.selectCount(Wrappers.lambdaQuery(AutomationReviewDO.class)
                .eq(AutomationReviewDO::getTaskId, taskId)
                .eq(AutomationReviewDO::getProjectId, projectId)
                .eq(AutomationReviewDO::getMergeRequestId, mergeRequestId)
                .eq(AutomationReviewDO::getStatus, MybatisAutomationRunRepository.REVIEWED));
        return count > 0;
    }

    @Override
    public Optional<Long> tryStartReview(
            final long taskId, final long runId, final ChangeRequestSummary request, final LocalDateTime now) {
        final AutomationReviewDO existing = this.reviewMapper.selectOne(Wrappers.lambdaQuery(AutomationReviewDO.class)
                .eq(AutomationReviewDO::getTaskId, taskId)
                .eq(AutomationReviewDO::getProjectId, request.projectId())
                .eq(AutomationReviewDO::getMergeRequestId, request.localId()));
        if (existing == null) {
            final AutomationReviewDO row = MybatisAutomationRunRepository.baseRow(taskId, runId, request);
            row.setStatus(AutomationReviewStatus.PROCESSING.value());
            try {
                this.reviewMapper.insert(row);
            } catch (final DuplicateKeyException ex) {
                // 另一个实例同时插入：唯一键兜底，本轮放弃。
                return Optional.empty();
            }
            return Optional.of(row.getId() == null ? 0L : row.getId());
        }
        final String status = existing.getStatus();
        if (AutomationReviewStatus.REVIEWED.value().equals(status)) {
            return Optional.empty();
        }
        if (AutomationReviewStatus.PROCESSING.value().equals(status)
                && existing.getUpdatedAt() != null
                && existing.getUpdatedAt().isAfter(now.minusMinutes(MybatisAutomationRunRepository.STALE_MINUTES))) {
            return Optional.empty();
        }
        // 失败记录或超时未完成的记录允许重新抢占；以旧状态做 CAS，避免两个实例同时抢到。
        final int rows = this.reviewMapper.update(
                null,
                Wrappers.<AutomationReviewDO>lambdaUpdate()
                        .set(AutomationReviewDO::getRunId, runId)
                        .set(AutomationReviewDO::getStatus, AutomationReviewStatus.PROCESSING.value())
                        .set(AutomationReviewDO::getUpdatedAt, now)
                        .eq(AutomationReviewDO::getId, existing.getId())
                        .eq(AutomationReviewDO::getStatus, status));
        return rows == 1 ? Optional.of(existing.getId()) : Optional.empty();
    }

    @Override
    public void completeReview(final long reviewId, final AutomationReview review) {
        this.reviewMapper.update(
                null,
                Wrappers.<AutomationReviewDO>lambdaUpdate()
                        .set(AutomationReviewDO::getStatus, review.status().value())
                        .set(AutomationReviewDO::getCommentText, review.commentText())
                        .set(AutomationReviewDO::getModel, MybatisAutomationRunRepository.clip(review.model(), 64))
                        .set(AutomationReviewDO::getFileCount, review.fileCount())
                        .set(AutomationReviewDO::getAddLines, review.addLines())
                        .set(AutomationReviewDO::getDelLines, review.delLines())
                        .set(
                                AutomationReviewDO::getErrorMessage,
                                MybatisAutomationRunRepository.clip(review.errorMessage(), 1000))
                        .eq(AutomationReviewDO::getId, reviewId));
        this.fileMapper.delete(
                Wrappers.lambdaQuery(AutomationReviewFileDO.class).eq(AutomationReviewFileDO::getReviewId, reviewId));
        for (final AutomationReviewFile file : review.files()) {
            final AutomationReviewFileDO fileRow = new AutomationReviewFileDO();
            fileRow.setReviewId(reviewId);
            fileRow.setPath(MybatisAutomationRunRepository.clip(file.path(), 500));
            fileRow.setAddLines(file.addLines());
            fileRow.setDelLines(file.delLines());
            fileRow.setLevel(file.level().value());
            fileRow.setCommentText(file.commentText());
            fileRow.setSkipReason(MybatisAutomationRunRepository.clip(file.skipReason(), 250));
            this.fileMapper.insert(fileRow);
        }
    }

    @Override
    public int failStaleRuns(final LocalDateTime cutoff) {
        return this.runMapper.update(
                null,
                Wrappers.<AutomationTaskRunDO>lambdaUpdate()
                        .set(AutomationTaskRunDO::getStatus, AutomationRunStatus.FAILED.value())
                        .set(AutomationTaskRunDO::getSummary, "执行中断（进程重启）")
                        .set(AutomationTaskRunDO::getFinishedAt, LocalDateTime.now(MybatisAutomationRunRepository.ZONE))
                        .eq(AutomationTaskRunDO::getStatus, AutomationRunStatus.RUNNING.value())
                        .lt(AutomationTaskRunDO::getStartedAt, cutoff));
    }

    @Override
    public void deleteReviewsByRun(final long runId) {
        final List<Long> reviewIds = this.reviewMapper
                .selectList(Wrappers.lambdaQuery(AutomationReviewDO.class).eq(AutomationReviewDO::getRunId, runId))
                .stream()
                .map(AutomationReviewDO::getId)
                .toList();
        if (reviewIds.isEmpty()) {
            return;
        }
        this.fileMapper.delete(
                Wrappers.lambdaQuery(AutomationReviewFileDO.class).in(AutomationReviewFileDO::getReviewId, reviewIds));
        this.reviewMapper.delete(
                Wrappers.lambdaQuery(AutomationReviewDO.class).eq(AutomationReviewDO::getRunId, runId));
    }

    @Override
    public void deleteRun(final long runId) {
        this.runMapper.deleteById(runId);
    }

    private static AutomationReviewDO baseRow(final long taskId, final long runId, final ChangeRequestSummary request) {
        final AutomationReviewDO row = new AutomationReviewDO();
        row.setTaskId(taskId);
        row.setRunId(runId);
        row.setProjectId(request.projectId());
        row.setRepositoryPath(MybatisAutomationRunRepository.clip(request.repositoryPath(), 128));
        row.setMergeRequestId(request.localId());
        row.setMergeRequestTitle(MybatisAutomationRunRepository.clip(request.title(), 250));
        row.setWebUrl(MybatisAutomationRunRepository.clip(request.webUrl(), 512));
        row.setAuthorName(MybatisAutomationRunRepository.clip(request.authorName(), 64));
        row.setSourceBranch(MybatisAutomationRunRepository.clip(request.sourceBranch(), 128));
        row.setTargetBranch(MybatisAutomationRunRepository.clip(request.targetBranch(), 128));
        row.setFileCount(0);
        row.setAddLines(0);
        row.setDelLines(0);
        return row;
    }

    private static AutomationTaskRun toDomain(final AutomationTaskRunDO row) {
        return new AutomationTaskRun(
                row.getId() == null ? 0L : row.getId(),
                row.getTaskId() == null ? 0L : row.getTaskId(),
                row.getTriggerType(),
                OutputMode.of(row.getOutputMode()),
                AutomationRunStatus.of(row.getStatus()).orElse(AutomationRunStatus.FAILED),
                row.getStartedAt() == null
                        ? LocalDateTime.now(MybatisAutomationRunRepository.ZONE)
                        : row.getStartedAt(),
                row.getFinishedAt(),
                row.getDurationMs(),
                row.getSummary() == null ? "" : row.getSummary(),
                row.getErrorMessage(),
                row.getMergeRequestCount() == null ? 0 : row.getMergeRequestCount(),
                row.getReviewedCount() == null ? 0 : row.getReviewedCount(),
                row.getCommentCount() == null ? 0 : row.getCommentCount());
    }

    private static AutomationReview toReview(final AutomationReviewDO row, final List<AutomationReviewFileDO> files) {
        final List<AutomationReviewFile> fileViews = new ArrayList<>();
        for (final AutomationReviewFileDO file : files) {
            fileViews.add(new AutomationReviewFile(
                    file.getId() == null ? 0L : file.getId(),
                    file.getReviewId() == null ? 0L : file.getReviewId(),
                    file.getPath() == null ? "" : file.getPath(),
                    file.getAddLines() == null ? 0 : file.getAddLines(),
                    file.getDelLines() == null ? 0 : file.getDelLines(),
                    ReviewFileLevel.of(file.getLevel()),
                    file.getCommentText(),
                    file.getSkipReason()));
        }
        return new AutomationReview(
                row.getId() == null ? 0L : row.getId(),
                row.getRunId() == null ? 0L : row.getRunId(),
                row.getTaskId() == null ? 0L : row.getTaskId(),
                row.getProjectId() == null ? "" : row.getProjectId(),
                row.getRepositoryPath() == null ? "" : row.getRepositoryPath(),
                row.getMergeRequestId() == null ? 0L : row.getMergeRequestId(),
                row.getMergeRequestTitle() == null ? "" : row.getMergeRequestTitle(),
                row.getWebUrl(),
                row.getAuthorName(),
                row.getSourceBranch(),
                row.getTargetBranch(),
                AutomationReviewStatus.of(row.getStatus()),
                row.getCommentText(),
                row.getModel(),
                row.getFileCount() == null ? 0 : row.getFileCount(),
                row.getAddLines() == null ? 0 : row.getAddLines(),
                row.getDelLines() == null ? 0 : row.getDelLines(),
                row.getErrorMessage(),
                fileViews);
    }

    private static @Nullable String clip(final @Nullable String value, final int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
