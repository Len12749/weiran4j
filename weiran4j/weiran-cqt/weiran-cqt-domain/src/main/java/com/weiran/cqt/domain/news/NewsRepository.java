package com.weiran.cqt.domain.news;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import java.util.Optional;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 大赛动态仓储端口。 */
@NullMarked
public interface NewsRepository {

    /** 按排序和 ID 升序分页；{@code homeOnly} 为 true 时只查首页展示项。 */
    PageResult<News> page(PageQuery query, @Nullable String keyword, boolean homeOnly);

    /** 按主键查询动态。 */
    Optional<News> findById(long id);

    /** 新增动态并返回主键。 */
    long insert(String title, String image, boolean showOnHome, int sort, String content);

    /** 全量更新动态。 */
    void update(long id, String title, String image, boolean showOnHome, int sort, String content);

    /** 删除动态。 */
    void delete(long id);
}
