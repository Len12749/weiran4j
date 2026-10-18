package com.weiran.cqt.api.entryexport;

import org.jspecify.annotations.Nullable;

public record EntryExportCommand(
        long competitionId,
        long phaseId,
        @Nullable String keyword,
        @Nullable String source,
        @Nullable Integer status) {}
