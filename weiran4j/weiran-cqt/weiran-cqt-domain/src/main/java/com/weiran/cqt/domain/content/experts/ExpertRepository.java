package com.weiran.cqt.domain.content.experts;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 学术评审团仓储。 */
public interface ExpertRepository {
    PageResult<Expert> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    Optional<Expert> findById(long id);

    long insert(Expert value);

    void update(long id, Expert value);

    void delete(long id);
}
