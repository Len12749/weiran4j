package com.weiran.system.api.siteconfig;

import java.util.List;
import org.jspecify.annotations.NullMarked;

/** 站点配置响应。 */
@NullMarked
public record SiteConfigResponse(List<SiteConfigItemView> items) {}
