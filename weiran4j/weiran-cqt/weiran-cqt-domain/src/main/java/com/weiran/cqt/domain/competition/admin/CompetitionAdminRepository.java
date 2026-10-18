package com.weiran.cqt.domain.competition.admin;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface CompetitionAdminRepository {
    PageResult<AdminCompetition> page(
            PageQuery query, @Nullable String keyword, @Nullable Integer year, @Nullable Integer status);

    Optional<AdminCompetition> find(long id);

    Optional<AdminCompetition> lock(long id);

    long create(CompetitionConfiguration value, LocalDateTime now);

    boolean update(long id, CompetitionConfiguration value, long expectedVersion, LocalDateTime now);

    boolean status(long id, int status, long expectedVersion, LocalDateTime now);

    void createDefaultGroups(long competitionId);
}
