package com.weiran.cqt.domain.content.showcases;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 佳作欣赏仓储。 */
public interface ShowcaseRepository {
    PageResult<Showcase> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    Optional<Showcase> findById(long id);

    long insert(Showcase value);

    void update(long id, Showcase value);

    void delete(long id);
}
