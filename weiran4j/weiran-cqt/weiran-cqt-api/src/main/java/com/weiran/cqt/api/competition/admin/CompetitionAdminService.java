package com.weiran.cqt.api.competition.admin;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;

public interface CompetitionAdminService {
    PageResult<CompetitionAdminView> page(
            PageQuery query, @Nullable String keyword, @Nullable Integer year, @Nullable Integer status);

    CompetitionAdminView get(long id);

    Result create(CompetitionAdminCommand command);

    Result update(long id, CompetitionAdminCommand command);

    Result status(long id, int status, long expectedVersion);

    record Result(long id, long version) {}
}
