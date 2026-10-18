package com.weiran.cqt.domain.content.resources;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** 指导资料仓储。 */
public interface ResourceRepository {
    PageResult<Resource> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    Optional<Resource> findById(long id);

    long insert(Resource value);

    void update(long id, Resource value);

    void delete(long id);
}
