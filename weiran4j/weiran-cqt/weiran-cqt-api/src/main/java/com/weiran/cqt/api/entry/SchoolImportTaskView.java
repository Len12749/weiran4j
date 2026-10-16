package com.weiran.cqt.api.entry;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** uniapp 可查询的导入任务及逐作品组结果。 */
public record SchoolImportTaskView(
        String id,
        long competitionId,
        String status,
        @Nullable String zipUrl,
        @Nullable String zipName,
        @Nullable String excelUrl,
        @Nullable String excelName,
        int totalRows,
        int processedRows,
        int successCount,
        int failureCount,
        @Nullable LocalDateTime createdAt,
        List<Map<String, @Nullable Object>> results) {}
