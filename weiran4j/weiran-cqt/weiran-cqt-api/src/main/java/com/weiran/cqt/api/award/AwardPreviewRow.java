package com.weiran.cqt.api.award;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** 预览行包含未命中与阻断，不泄露评分评语或证件。 */
public record AwardPreviewRow(
        long targetId,
        long entryId,
        @Nullable Long participantId,
        String targetType,
        String name,
        String title,
        @Nullable BigDecimal score,
        long finalVersion,
        @Nullable Long ruleId,
        @Nullable Long ruleRevision,
        @Nullable String awardName,
        String status,
        @Nullable String reason) {}
