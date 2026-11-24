package com.wulicode.weiran.automation.domain.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 执行记录与评审结果仓储端口。 */
public interface AutomationRunRepository {

    /** 新增一条执行记录并返回 ID；记录以「执行未完成」占位，结束时用 {@link #finishRun} 收尾。 */
    long createRun(AutomationTaskRun run);

    /** 收尾执行记录：写入终态、耗时、摘要、失败原因与计数。 */
    void finishRun(
            long id,
            AutomationRunStatus status,
            LocalDateTime finishedAt,
            long durationMs,
            String summary,
            @Nullable String errorMessage,
            int mergeRequestCount,
            int reviewedCount,
            int commentCount);

    /** 某个任务的执行记录，按开始时间倒序。 */
    List<AutomationTaskRun> findRuns(long taskId, long offset, int limit);

    /** 某个任务的执行记录总数。 */
    long countRuns(long taskId);

    /** 按 ID 查找执行记录。 */
    Optional<AutomationTaskRun> findRun(long id);

    /** 某次执行的评审结果（含逐文件意见），按 ID 升序。 */
    List<AutomationReview> findReviews(long runId);

    /** 本地是否已经对该合并请求生成过成功评论（用于在拉取阶段就过滤掉）。 */
    boolean existsReviewed(long taskId, String projectId, long mergeRequestId);

    /**
     * 抢占一个合并请求的评审权。
     *
     * <p>靠 {@code (task_id, project_id, merge_request_id)} 唯一键兜底：正在处理中的合并请求、已成功的合并请求
     * 都返回空；只有失败记录或超时未完成（进程崩溃）的抢占请求可以重新进入处理。
     *
     * @return 评审记录 ID；抢占失败时为空
     */
    Optional<Long> tryStartReview(long taskId, long runId, ChangeRequestSummary request, LocalDateTime now);

    /** 完成一次评审：写入终态、评论正文与逐文件意见（覆盖旧明细）。 */
    void completeReview(long reviewId, AutomationReview review);

    /** 把启动前没有收尾的执行记录标记为中断；返回受影响行数。 */
    int failStaleRuns(LocalDateTime cutoff);

    /** 删除一次执行产生的全部评审结果（含逐文件意见）。 */
    void deleteReviewsByRun(long runId);

    /** 删除一条执行记录。 */
    void deleteRun(long runId);
}
