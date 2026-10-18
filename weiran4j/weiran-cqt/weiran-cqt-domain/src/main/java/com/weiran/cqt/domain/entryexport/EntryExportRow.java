package com.weiran.cqt.domain.entryexport;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

public record EntryExportRow(
        long competitionId,
        String competition,
        long phaseId,
        String phaseCode,
        String phaseName,
        long entryId,
        String entryNo,
        String title,
        String source,
        @Nullable Long targetId,
        String targetType,
        @Nullable Long participantId,
        String participantName,
        String region,
        String category,
        String major,
        String groupName,
        int reviewStatus,
        String targetStatus,
        String scoreStatus,
        @Nullable BigDecimal score,
        @Nullable Long scoreVersion) {}
