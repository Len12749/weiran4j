package com.weiran.cqt.domain.entry;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 一件作品及其连续成员行，或无法导入的一组源行。 */
public record SchoolImportGroup(
        int firstSourceRow,
        int lastSourceRow,
        List<SchoolImportRow> rows,
        @Nullable String errorMessage) {

    /** 是否包含可导入作品。 */
    public boolean isValid() {
        return this.errorMessage == null;
    }

    /** 是否团体作品。 */
    public boolean isTeam() {
        return !this.rows.isEmpty() && this.rows.getFirst().entryType() == SchoolImportEntryType.TEAM;
    }
}
