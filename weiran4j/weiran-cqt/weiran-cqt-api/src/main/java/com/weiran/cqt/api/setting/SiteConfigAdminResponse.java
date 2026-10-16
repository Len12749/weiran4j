package com.weiran.cqt.api.setting;

import java.util.List;

/** 站点配置后台表单读取结果。 */
public record SiteConfigAdminResponse(List<SiteConfigAdminItem> items) {}
