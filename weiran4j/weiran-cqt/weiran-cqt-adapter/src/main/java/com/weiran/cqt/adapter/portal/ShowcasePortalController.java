package com.weiran.cqt.adapter.portal;

import com.weiran.cqt.api.content.showcases.ShowcaseService;
import com.weiran.cqt.api.content.showcases.ShowcaseView;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 佳作欣赏公开查询；只公开启用项。 */
@PortalController
@RequestMapping("/api-web/n/content/showcases")
public class ShowcasePortalController {
    private final ShowcaseService service;

    public ShowcasePortalController(final ShowcaseService service) {
        this.service = service;
    }

    @GetMapping
    public PortalResult<PageResult<ShowcaseView>> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String category) {
        return PortalResult.ok(this.service.page(new PageQuery(page, pageSize), null, category, true, false));
    }

    @GetMapping("/{id}")
    public PortalResult<ShowcaseView> get(@PathVariable("id") final long id) {
        return PortalResult.ok(this.service.getPublic(id));
    }
}
