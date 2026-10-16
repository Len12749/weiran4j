package com.weiran.cqt.domain.entry;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 持久化的上传会话与作品导入任务。 */
public record SchoolImportTask(
        String id,
        long accountId,
        @Nullable String schoolId,
        @Nullable Long competitionId,
        @Nullable String zipUrl,
        @Nullable String zipName,
        @Nullable String excelUrl,
        @Nullable String excelName,
        @Nullable String importType,
        SchoolImportTaskStatus status,
        int totalRows,
        int processedRows,
        int successCount,
        int failureCount,
        @Nullable LocalDateTime leaseUntil,
        @Nullable LocalDateTime uploadExpiresAt,
        int attemptCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
