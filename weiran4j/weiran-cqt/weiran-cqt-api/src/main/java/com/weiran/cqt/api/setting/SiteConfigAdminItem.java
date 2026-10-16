package com.weiran.cqt.api.setting;

import org.jspecify.annotations.Nullable;

/** 站点配置后台表单项。 */
public record SiteConfigAdminItem(
        String key, String label, int ident, @Nullable String contents) {}
