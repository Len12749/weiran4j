package com.weiran.cqt.api.content.resources;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;

/** 指导资料用例。 */
public interface ResourceService {
    PageResult<ResourceView> page(
            PageQuery query,
            @Nullable String keyword,
            @Nullable String category,
            @Nullable Boolean enabled,
            boolean homeOnly);

    ResourceView get(long id);

    ResourceView getPublic(long id);

    long create(ResourceCommand command);

    void update(long id, ResourceCommand command);

    void delete(long id);
}
