package com.weiran.cqt.domain.review;

import java.math.BigDecimal;

/** 工作流不可变快照。 */
public record AwardCandidate(
        long targetId,
        long entryId,
        @org.jspecify.annotations.Nullable Long participantId,
        String targetType,
        long firstCategoryId,
        @org.jspecify.annotations.Nullable Long secondCategoryId,
        String groupName,
        String displayName,
        String title,
        @org.jspecify.annotations.Nullable BigDecimal score,
        long finalVersion,
        String finalState,
        long targetVersion,
        boolean eligible,
        String blockedReason) {}
