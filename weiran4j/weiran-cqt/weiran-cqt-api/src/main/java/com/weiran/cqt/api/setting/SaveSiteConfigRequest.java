package com.weiran.cqt.api.setting;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 站点配置后台表单全量保存请求。 */
public record SaveSiteConfigRequest(@Nullable List<SiteConfigValue> items) {}
