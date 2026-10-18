package com.weiran.system.domain.siteconfig;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 站点配置原始值。 */
@NullMarked
public record SiteConfigValue(
        String key, String label, int ident, @Nullable String contents) {}
