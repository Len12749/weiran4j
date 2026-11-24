package com.wulicode.weiran.automation.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wulicode.weiran.automation.domain.task.AutomationRunStatus;
import com.wulicode.weiran.automation.domain.task.AutomationTask;
import com.wulicode.weiran.automation.domain.task.AutomationTaskRepository;
import com.wulicode.weiran.automation.domain.task.OutputMode;
import com.wulicode.weiran.automation.infrastructure.persistence.entity.AutomationTaskDO;
import com.wulicode.weiran.automation.infrastructure.persistence.mapper.AutomationTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** {@link AutomationTaskRepository} 的 MyBatis-Plus 实现。 */
public class MybatisAutomationTaskRepository implements AutomationTaskRepository {

    private final AutomationTaskMapper mapper;

    /** 构造任务仓储。 */
    public MybatisAutomationTaskRepository(final AutomationTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<AutomationTask> findAll() {
        return this.mapper
                .selectList(Wrappers.lambdaQuery(AutomationTaskDO.class).orderByAsc(AutomationTaskDO::getId))
                .stream()
                .map(MybatisAutomationTaskRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<AutomationTask> findById(final long id) {
        return Optional.ofNullable(this.mapper.selectById(id)).map(MybatisAutomationTaskRepository::toDomain);
    }

    @Override
    public void updateEnabled(final long id, final boolean enabled) {
        this.mapper.update(
                null,
                Wrappers.<AutomationTaskDO>lambdaUpdate()
                        .set(AutomationTaskDO::getEnabled, enabled ? 1 : 0)
                        .eq(AutomationTaskDO::getId, id));
    }

    @Override
    public boolean claim(
            final long id,
            final LocalDateTime now,
            final LocalDateTime nextRunAt,
            final LocalDateTime leaseUntil,
            final boolean requireDue) {
        final LambdaUpdateWrapper<AutomationTaskDO> update = Wrappers.<AutomationTaskDO>lambdaUpdate()
                .set(AutomationTaskDO::getLockedUntil, leaseUntil)
                .set(AutomationTaskDO::getNextRunAt, nextRunAt)
                .eq(AutomationTaskDO::getId, id)
                .eq(AutomationTaskDO::getEnabled, 1)
                .and(wrapper -> wrapper.isNull(AutomationTaskDO::getLockedUntil)
                        .or()
                        .lt(AutomationTaskDO::getLockedUntil, now));
        if (requireDue) {
            update.and(wrapper ->
                    wrapper.isNull(AutomationTaskDO::getNextRunAt).or().le(AutomationTaskDO::getNextRunAt, now));
        }
        return this.mapper.update(null, update) == 1;
    }

    @Override
    public void release(final long id) {
        this.mapper.update(
                null,
                Wrappers.<AutomationTaskDO>lambdaUpdate()
                        .set(AutomationTaskDO::getLockedUntil, null)
                        .eq(AutomationTaskDO::getId, id));
    }

    @Override
    public void recordRunResult(
            final long id,
            final long runId,
            final AutomationRunStatus status,
            final String summary,
            final LocalDateTime runAt) {
        this.mapper.update(
                null,
                Wrappers.<AutomationTaskDO>lambdaUpdate()
                        .set(AutomationTaskDO::getLastRunAt, runAt)
                        .set(AutomationTaskDO::getLastRunStatus, status.value())
                        .set(AutomationTaskDO::getLastRunSummary, summary)
                        .set(AutomationTaskDO::getLastRunId, runId)
                        .eq(AutomationTaskDO::getId, id));
    }

    @Override
    public void recordCheckResult(
            final long id, final AutomationRunStatus status, final String summary, final LocalDateTime runAt) {
        this.mapper.update(
                null,
                Wrappers.<AutomationTaskDO>lambdaUpdate()
                        .set(AutomationTaskDO::getLastRunAt, runAt)
                        .set(AutomationTaskDO::getLastRunStatus, status.value())
                        .set(AutomationTaskDO::getLastRunSummary, summary)
                        .eq(AutomationTaskDO::getId, id));
    }

    @Override
    public void clearLastRun(final long id) {
        this.mapper.update(
                null,
                Wrappers.<AutomationTaskDO>lambdaUpdate()
                        .set(AutomationTaskDO::getLastRunAt, null)
                        .set(AutomationTaskDO::getLastRunStatus, null)
                        .set(AutomationTaskDO::getLastRunSummary, null)
                        .set(AutomationTaskDO::getLastRunId, null)
                        .eq(AutomationTaskDO::getId, id));
    }

    private static AutomationTask toDomain(final AutomationTaskDO row) {
        return new AutomationTask(
                row.getId() == null ? 0L : row.getId(),
                row.getCode(),
                row.getName(),
                row.getDescription(),
                row.getTriggerDescription(),
                row.getScheduleDescription(),
                row.getScheduleKind(),
                row.getIntervalSeconds() == null ? 60 : row.getIntervalSeconds(),
                OutputMode.of(row.getOutputMode()),
                row.getEnabled() != null && row.getEnabled() == 1,
                row.getLastRunAt(),
                row.getLastRunStatus(),
                row.getLastRunSummary(),
                row.getLastRunId(),
                row.getNextRunAt(),
                row.getLockedUntil(),
                row.getCreatedAt(),
                row.getUpdatedAt());
    }
}
