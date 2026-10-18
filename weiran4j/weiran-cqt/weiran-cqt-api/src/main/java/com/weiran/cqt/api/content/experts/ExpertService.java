package com.weiran.cqt.api.content.experts;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;

/** 学术评审团用例。 */
public interface ExpertService {
    PageResult<ExpertView> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    ExpertView get(long id);

    ExpertView getPublic(long id);

    long create(ExpertCommand command);

    void update(long id, ExpertCommand command);

    void delete(long id);
}
