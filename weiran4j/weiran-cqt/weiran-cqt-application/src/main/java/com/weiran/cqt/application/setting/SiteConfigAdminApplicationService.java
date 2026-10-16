package com.weiran.cqt.application.setting;

import com.weiran.cqt.api.setting.SiteConfigAdminItem;
import com.weiran.cqt.api.setting.SiteConfigAdminService;
import com.weiran.cqt.api.setting.SiteConfigValue;
import com.weiran.cqt.domain.setting.SettingRepository;
import com.weiran.cqt.domain.setting.SiteConfig;
import com.weiran.framework.error.BizException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;

/** 站点配置管理用例：保护固定配置目录并原样读写 contents。 */
public class SiteConfigAdminApplicationService implements SiteConfigAdminService {

    private final SettingRepository settingRepository;

    /** 构造管理服务。 */
    public SiteConfigAdminApplicationService(final SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public List<SiteConfigAdminItem> getEditableConfig() {
        final Map<Integer, @Nullable String> contents =
                this.settingRepository.findContentsByIdentRange(SiteConfig.IDENT_FROM, SiteConfig.IDENT_TO);
        return SiteConfig.definitions().stream()
                .map(definition -> new SiteConfigAdminItem(
                        definition.key(), definition.label(), definition.ident(), contents.get(definition.ident())))
                .toList();
    }

    @Override
    @Transactional
    public void saveConfig(final @Nullable List<SiteConfigValue> items) {
        if (items == null) {
            throw BizException.badRequest("必须提交全部站点配置项");
        }
        final Set<String> submittedKeys = new HashSet<>();
        for (final @Nullable SiteConfigValue item : items) {
            if (item == null) {
                throw BizException.badRequest("站点配置项无效");
            }
            SiteConfigAdminApplicationService.identForKey(item.key());
            if (!submittedKeys.add(item.key())) {
                throw BizException.badRequest("站点配置项无效或重复");
            }
        }
        if (submittedKeys.size() != SiteConfig.definitions().size()) {
            throw BizException.badRequest("必须提交全部站点配置项");
        }
        for (final SiteConfigValue item : items) {
            this.settingRepository.saveContents(
                    SiteConfigAdminApplicationService.identForKey(item.key()), item.contents());
        }
    }

    private static int identForKey(final @Nullable String key) {
        if (key == null) {
            throw BizException.badRequest("站点配置项无效");
        }
        final Integer ident = SiteConfig.identForKey(key);
        if (ident == null) {
            throw BizException.badRequest("站点配置项无效");
        }
        return ident.intValue();
    }
}
