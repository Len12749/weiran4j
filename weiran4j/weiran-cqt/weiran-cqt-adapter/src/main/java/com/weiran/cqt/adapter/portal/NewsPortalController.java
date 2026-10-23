package com.weiran.cqt.adapter.portal;

import com.weiran.cqt.api.news.NewsPortalService;
import com.weiran.cqt.api.news.NewsView;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 大赛动态前台公开接口。 */
@PortalController
@RequestMapping("/api-web/n/product")
public class NewsPortalController {

    private final NewsPortalService newsService;

    /** 构造控制器。 */
    public NewsPortalController(final NewsPortalService newsService) {
        this.newsService = newsService;
    }

    /** 动态列表；首页可通过 {@code homeOnly=true} 限定首页展示项。 */
    @GetMapping("/news")
    public PortalResult<PageResult<NewsView>> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(defaultValue = "false") final boolean homeOnly) {
        return PortalResult.ok(this.newsService.page(new PageQuery(page, pageSize), homeOnly));
    }

    /** 动态详情（保留 uniapp 原有路径与参数形式）。 */
    @GetMapping("/newsdetail")
    public PortalResult<NewsView> detail(@RequestParam("id") final long id) {
        return PortalResult.ok(this.newsService.get(id));
    }
}
