package com.weiran.cqt.adapter.portal;

import com.weiran.cqt.api.content.experts.ExpertService;
import com.weiran.cqt.api.content.experts.ExpertView;
import com.weiran.framework.page.PageQuery;
import com.weiran.framework.page.PageResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** 学术评审团公开查询；只公开启用项。 */
@PortalController
@RequestMapping("/api-web/n/content/experts")
public class ExpertPortalController {
    private final ExpertService service;

    public ExpertPortalController(final ExpertService service) {
        this.service = service;
    }

    @GetMapping
    public PortalResult<PageResult<ExpertView>> page(
            @RequestParam(defaultValue = "1") final int page,
            @RequestParam(defaultValue = "20") final int pageSize,
            @RequestParam(defaultValue = "false") final boolean homeOnly) {
        return PortalResult.ok(this.service.page(new PageQuery(page, pageSize), null, null, true, homeOnly));
    }

    @GetMapping("/{id}")
    public PortalResult<ExpertView> get(@PathVariable("id") final long id) {
        return PortalResult.ok(this.service.getPublic(id));
    }
}
