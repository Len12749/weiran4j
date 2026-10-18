package com.weiran.cqt.api.news;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 大赛动态管理用例契约。 */
@NullMarked
public interface NewsAdminService {

    /** 分页查询动态。 */
    PageResult<NewsView> page(PageQuery query, @Nullable String keyword);

    /** 读取动态详情。 */
    NewsView get(long id);

    /** 新增动态。 */
    long create(NewsCommand command);

    /** 更新动态。 */
    void update(long id, NewsCommand command);

    /** 删除动态。 */
    void delete(long id);
}
