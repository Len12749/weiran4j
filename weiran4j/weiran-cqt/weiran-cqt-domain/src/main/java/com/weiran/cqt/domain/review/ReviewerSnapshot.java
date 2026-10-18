package com.weiran.cqt.domain.review;

import java.time.LocalDateTime;

/** 工作流不可变快照。 */
public record ReviewerSnapshot(
        long userId,
        String state,
        @org.jspecify.annotations.Nullable LocalDateTime updatedAt) {}
