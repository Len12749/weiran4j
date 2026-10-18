package com.weiran.cqt.api.award;

import java.time.LocalDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record AwardBatchView(
        long id,
        long phaseId,
        String status,
        long inputVersion,
        String previewId,
        @Nullable Long supersedesBatchId,
        @Nullable String reason,
        long version,
        long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AwardPreviewRow> rows) {}
