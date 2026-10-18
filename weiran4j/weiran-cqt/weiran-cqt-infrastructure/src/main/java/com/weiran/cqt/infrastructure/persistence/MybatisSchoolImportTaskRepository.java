package com.weiran.cqt.infrastructure.persistence;

import com.weiran.cqt.domain.entry.SchoolImportRowResult;
import com.weiran.cqt.domain.entry.SchoolImportTask;
import com.weiran.cqt.domain.entry.SchoolImportTaskRepository;
import com.weiran.cqt.domain.entry.SchoolImportTaskStatus;
import com.weiran.cqt.infrastructure.persistence.entity.CqtSchoolImportRowDO;
import com.weiran.cqt.infrastructure.persistence.entity.CqtSchoolImportTaskDO;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtSchoolImportRowMapper;
import com.weiran.cqt.infrastructure.persistence.mapper.CqtSchoolImportTaskMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

/** {@link SchoolImportTaskRepository} 的 MyBatis 实现。 */
public class MybatisSchoolImportTaskRepository implements SchoolImportTaskRepository {

    private final CqtSchoolImportTaskMapper taskMapper;

    private final CqtSchoolImportRowMapper rowMapper;

    /** 构造仓储。 */
    public MybatisSchoolImportTaskRepository(
            final CqtSchoolImportTaskMapper taskMapper, final CqtSchoolImportRowMapper rowMapper) {
        this.taskMapper = taskMapper;
        this.rowMapper = rowMapper;
    }

    @Override
    public void create(final SchoolImportTask task) {
        final CqtSchoolImportTaskDO row = new CqtSchoolImportTaskDO();
        row.setId(task.id());
        row.setAccountId(task.accountId());
        row.setSchoolId(task.schoolId());
        row.setCompetitionId(task.competitionId());
        row.setZipUrl(task.zipUrl());
        row.setZipName(task.zipName());
        row.setExcelUrl(task.excelUrl());
        row.setExcelName(task.excelName());
        row.setImportType(task.importType());
        row.setStatus(task.status().name());
        row.setErrorMessage(task.errorMessage());
        row.setTotalRows(task.totalRows());
        row.setProcessedRows(task.processedRows());
        row.setSuccessCount(task.successCount());
        row.setFailureCount(task.failureCount());
        row.setLeaseUntil(task.leaseUntil());
        row.setUploadExpiresAt(task.uploadExpiresAt());
        row.setAttemptCount(task.attemptCount());
        row.setCreatedAt(task.createdAt());
        row.setUpdatedAt(task.updatedAt());
        this.taskMapper.insert(row);
    }

    @Override
    public Optional<SchoolImportTask> findOwned(final String taskId, final long accountId) {
        return Optional.ofNullable(this.taskMapper.findOwned(taskId, accountId))
                .map(MybatisSchoolImportTaskRepository::task);
    }

    @Override
    public boolean updateUpload(
            final String taskId,
            final long accountId,
            final @Nullable String zipUrl,
            final @Nullable String zipName,
            final @Nullable String excelUrl,
            final @Nullable String excelName,
            final LocalDateTime now) {
        return this.taskMapper.updateUpload(taskId, accountId, zipUrl, zipName, excelUrl, excelName, now) > 0;
    }

    @Override
    public boolean queue(
            final String taskId,
            final long accountId,
            final long competitionId,
            final String importType,
            final LocalDateTime now) {
        return this.taskMapper.queue(taskId, accountId, competitionId, importType, now) > 0;
    }

    @Override
    @Transactional
    public Optional<SchoolImportTask> claimNext(final LocalDateTime now, final LocalDateTime leaseUntil) {
        final CqtSchoolImportTaskDO row = this.taskMapper.lockNext(now);
        if (row == null || this.taskMapper.claim(row.getId(), leaseUntil, now) == 0) {
            return Optional.empty();
        }
        row.setStatus(SchoolImportTaskStatus.PROCESSING.name());
        row.setLeaseUntil(leaseUntil);
        row.setAttemptCount(row.getAttemptCount() + 1);
        row.setUpdatedAt(now);
        return Optional.of(MybatisSchoolImportTaskRepository.task(row));
    }

    @Override
    @Transactional
    public void insertRows(
            final String taskId, final List<SchoolImportRowResult> rows, final int totalRows, final LocalDateTime now) {
        this.taskMapper.setTotalRows(taskId, totalRows, now);
        for (final SchoolImportRowResult result : rows) {
            final CqtSchoolImportRowDO row = new CqtSchoolImportRowDO();
            row.setTaskId(taskId);
            row.setSourceRow(result.sourceRow());
            row.setLastSourceRow(result.lastSourceRow());
            row.setEntryFingerprint(result.entryFingerprint());
            row.setEntryId(result.entryId());
            row.setStatus(result.status());
            row.setDisplayJson(result.displayJson());
            row.setErrorMessage(result.errorMessage());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            try {
                this.rowMapper.insert(row);
            } catch (final DuplicateKeyException ignored) {
                // 已持久化的任务在租约恢复时复用原有行结果。
            }
        }
        this.taskMapper.syncCounts(taskId, now);
    }

    @Override
    public List<SchoolImportRowResult> rows(final String taskId) {
        return this.rowMapper.findByTask(taskId).stream()
                .map(row -> new SchoolImportRowResult(
                        row.getId(),
                        row.getTaskId(),
                        row.getSourceRow(),
                        row.getLastSourceRow(),
                        row.getEntryFingerprint(),
                        row.getEntryId(),
                        row.getStatus(),
                        row.getDisplayJson(),
                        row.getErrorMessage()))
                .toList();
    }

    @Override
    @Transactional
    public void markSuccess(
            final String taskId,
            final int firstSourceRow,
            final int lastSourceRow,
            final String fingerprint,
            final long entryId,
            final LocalDateTime now) {
        this.rowMapper.markSuccess(taskId, firstSourceRow, lastSourceRow, fingerprint, entryId);
        this.taskMapper.syncCounts(taskId, now);
    }

    @Override
    @Transactional
    public void markFailure(
            final String taskId,
            final int firstSourceRow,
            final int lastSourceRow,
            final String error,
            final LocalDateTime now) {
        this.rowMapper.markFailure(taskId, firstSourceRow, lastSourceRow, error);
        this.taskMapper.syncCounts(taskId, now);
    }

    @Override
    public void finish(final String taskId, final SchoolImportTaskStatus status, final LocalDateTime now) {
        this.taskMapper.finish(taskId, status.name(), now);
    }

    @Override
    @Transactional
    public void fail(
            final String taskId, final SchoolImportTaskStatus status, final String error, final LocalDateTime now) {
        this.taskMapper.finish(taskId, status.name(), now);
        this.taskMapper.setError(taskId, error);
    }

    @Override
    public List<SchoolImportTask> listOwned(final long accountId, final long competitionId) {
        return this.taskMapper.listOwned(accountId, competitionId).stream()
                .map(MybatisSchoolImportTaskRepository::task)
                .toList();
    }

    @Override
    public boolean hasPending(final long accountId, final @Nullable Long competitionId, final LocalDateTime now) {
        return this.taskMapper.hasPending(accountId, competitionId, now);
    }

    @Override
    public int expireUploading(final LocalDateTime now) {
        return this.taskMapper.expireUploading(now);
    }

    private static SchoolImportTask task(final CqtSchoolImportTaskDO row) {
        return new SchoolImportTask(
                row.getId(),
                row.getAccountId(),
                row.getSchoolId(),
                row.getCompetitionId(),
                row.getZipUrl(),
                row.getZipName(),
                row.getExcelUrl(),
                row.getExcelName(),
                row.getImportType(),
                SchoolImportTaskStatus.valueOf(row.getStatus()),
                row.getTotalRows(),
                row.getProcessedRows(),
                row.getSuccessCount(),
                row.getFailureCount(),
                row.getLeaseUntil(),
                row.getUploadExpiresAt(),
                row.getAttemptCount(),
                row.getCreatedAt(),
                row.getUpdatedAt(),
                row.getErrorMessage());
    }
}
