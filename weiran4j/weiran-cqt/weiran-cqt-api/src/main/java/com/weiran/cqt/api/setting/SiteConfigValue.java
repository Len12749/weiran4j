package com.weiran.cqt.api.setting;

import org.jspecify.annotations.Nullable;

/** 站点配置后台保存项。客户端只提交键与原始内容，不允许自行指定 ident。 */
public record SiteConfigValue(String key, @Nullable String contents) {}
