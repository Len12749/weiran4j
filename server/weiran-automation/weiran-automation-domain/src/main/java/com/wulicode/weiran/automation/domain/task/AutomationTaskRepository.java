package com.wulicode.weiran.automation.domain.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 自动化任务仓储端口。 */
public interface AutomationTaskRepository {

    /** 全部任务，按 ID 升序。 */
    List<AutomationTask> findAll();

    /** 按 ID 查找任务。 */
    Optional<AutomationTask> findById(long id);

    /** 修改启停状态。 */
    void updateEnabled(long id, boolean enabled);

    /**
     * 抢占执行租约：只有启用中、到达下次执行时间且租约已过期或未占用的任务返回 {@code true}。
     *
     * <p>多个实例同时 tick 时，数据库行级更新保证只有一个实例进入执行；同时把下次执行时间推到
     * {@code nextRunAt}，重启后不会丢调度节奏。
     *
     * @param requireDue 是否必须已到 {@code next_run_at}；手动触发传 {@code false}，用于跳过调度基线
     */
    boolean claim(long id, LocalDateTime now, LocalDateTime nextRunAt, LocalDateTime leaseUntil, boolean requireDue);

    /** 释放执行租约。 */
    void release(long id);

    /** 回写最近一次执行结果（含执行记录 ID）。 */
    void recordRunResult(long id, long runId, AutomationRunStatus status, String summary, LocalDateTime runAt);

    /** 回写最近一次检查结果，不改变最近执行记录 ID（本轮没有产生执行记录时使用）。 */
    void recordCheckResult(long id, AutomationRunStatus status, String summary, LocalDateTime runAt);

    /** 清空最近执行信息（执行记录被全部删除后使用）。 */
    void clearLastRun(long id);
}
