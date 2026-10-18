package com.weiran.system.adapter.web;

import com.weiran.framework.auth.RequiresPermission;
import com.weiran.framework.log.OperationLog;
import com.weiran.framework.response.ApiResponse;
import com.weiran.system.api.siteconfig.SaveSiteConfigRequest;
import com.weiran.system.api.siteconfig.SiteConfigResponse;
import com.weiran.system.api.siteconfig.SiteConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 站点配置管理接口。 */
@RestController
@RequestMapping("/api/system/site-config")
public class SiteConfigController {

    private final SiteConfigService service;

    public SiteConfigController(final SiteConfigService service) {
        this.service = service;
    }

    @GetMapping
    @RequiresPermission("system:site-config:list")
    public SiteConfigResponse getAll() {
        return new SiteConfigResponse(this.service.getAll());
    }

    @PutMapping
    @RequiresPermission("system:site-config:update")
    @OperationLog(module = "站点配置", description = "保存站点配置")
    public ApiResponse<Void> save(@RequestBody final SaveSiteConfigRequest request) {
        this.service.save(request.items());
        return ApiResponse.ok();
    }
}
