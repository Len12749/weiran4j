package com.weiran.cqt.api.setting;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** 站点配置后台表单服务。 */
public interface SiteConfigAdminService {

    /** 读取固定配置目录及数据库原始内容。 */
    List<SiteConfigAdminItem> getEditableConfig();

    /** 全量保存后台提交的配置内容。 */
    void saveConfig(@Nullable List<SiteConfigValue> items);
}
