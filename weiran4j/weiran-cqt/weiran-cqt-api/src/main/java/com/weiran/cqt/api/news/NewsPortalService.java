package com.weiran.cqt.api.news;

import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.NullMarked;

/** 大赛动态前台读取契约。 */
@NullMarked
public interface NewsPortalService {

    /** 查询动态；首页仅返回配置为首页展示的记录。 */
    PageResult<NewsView> page(PageQuery query, boolean homeOnly);

    /** 读取公开动态详情。 */
    NewsView get(long id);
}
