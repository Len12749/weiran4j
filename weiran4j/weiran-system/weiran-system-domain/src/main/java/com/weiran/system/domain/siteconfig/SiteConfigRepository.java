package com.weiran.system.domain.siteconfig;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/** 站点配置仓储端口。 */
public interface SiteConfigRepository {

    Map<Integer, @Nullable String> findContents();

    Map<Integer, @Nullable String> lockAndFindContents();

    void saveContents(int ident, @Nullable String contents);
}
