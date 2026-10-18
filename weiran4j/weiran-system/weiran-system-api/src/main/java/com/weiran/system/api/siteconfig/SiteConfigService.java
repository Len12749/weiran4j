package com.weiran.system.api.siteconfig;

import java.util.List;

/** 站点配置管理服务。 */
public interface SiteConfigService {

    List<SiteConfigItemView> getAll();

    void save(List<SiteConfigValueRequest> items);
}
