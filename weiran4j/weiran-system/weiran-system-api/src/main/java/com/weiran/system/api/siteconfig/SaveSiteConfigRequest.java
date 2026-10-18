package com.weiran.system.api.siteconfig;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 站点配置全量保存请求。 */
@NullMarked
public record SaveSiteConfigRequest(List<SiteConfigValueRequest> items) {}
