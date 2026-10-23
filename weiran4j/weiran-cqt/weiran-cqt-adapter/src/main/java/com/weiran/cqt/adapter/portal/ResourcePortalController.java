package com.weiran.cqt.adapter.portal;

import com.weiran.cqt.api.content.resources.ResourceService;
import com.weiran.cqt.api.content.resources.ResourceView;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 指导资料公开查询；只公开启用项。 */
@PortalController
@RequestMapping("/api-web/n/content/resources")
public class ResourcePortalController {
    private final ResourceService service;

    public ResourcePortalController(final ResourceService service) {
        this.service = service;
    }

    @GetMapping
    public PortalResult<PageResult<ResourceView>> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(required = false) final @Nullable String category) {
        return PortalResult.ok(this.service.page(new PageQuery(page, pageSize), null, category, true, false));
    }

    @GetMapping("/{id}")
    public PortalResult<ResourceView> get(@PathVariable("id") final long id) {
        return PortalResult.ok(this.service.getPublic(id));
    }
}
