package com.weiran.cqt.domain.entryexport;

import java.util.List;
import org.jspecify.annotations.Nullable;

public interface EntryExportRepository {
    List<EntryExportRow> rows(
            long competitionId,
            long phaseId,
            @Nullable String keyword,
            @Nullable String source,
            @Nullable Integer status);
}
