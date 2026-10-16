package com.weiran.cqt.domain.entry;

import org.jspecify.annotations.Nullable;

/** 一件作品组的持久化导入结果。 */
public record SchoolImportRowResult(
        long id,
        String taskId,
        int sourceRow,
        int lastSourceRow,
        @Nullable String entryFingerprint,
        @Nullable Long entryId,
        String status,
        @Nullable String displayJson,
        @Nullable String errorMessage) {}
