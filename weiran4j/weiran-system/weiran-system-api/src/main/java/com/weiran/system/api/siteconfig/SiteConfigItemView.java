package com.weiran.system.api.siteconfig;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 站点配置项视图。 */
@NullMarked
public record SiteConfigItemView(
        String key, String label, int ident, @Nullable String contents) {}
