package com.weiran.system.api.siteconfig;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** 站点配置写入项。 */
@NullMarked
public record SiteConfigValueRequest(String key, @Nullable String contents) {}
