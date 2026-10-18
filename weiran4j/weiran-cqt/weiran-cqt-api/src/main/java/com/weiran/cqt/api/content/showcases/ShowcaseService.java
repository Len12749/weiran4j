package com.weiran.cqt.api.content.showcases;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;

/** 佳作欣赏用例。 */
public interface ShowcaseService {
    PageResult<ShowcaseView> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    ShowcaseView get(long id);

    ShowcaseView getPublic(long id);

    long create(ShowcaseCommand command);

    void update(long id, ShowcaseCommand command);

    void delete(long id);
}
