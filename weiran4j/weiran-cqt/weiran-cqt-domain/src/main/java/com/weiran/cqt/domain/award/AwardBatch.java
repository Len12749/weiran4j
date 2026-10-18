package com.weiran.cqt.domain.award;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 批次输入和输出永久保留，状态变更使用version。 */
public record AwardBatch(
        long id,
        long phaseId,
        String status,
        long inputVersion,
        String previewId,
        @Nullable Long supersedesBatchId,
        @Nullable String reason,
        long version,
        long createdBy,
        String candidateJson,
        String snapshotJson,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
