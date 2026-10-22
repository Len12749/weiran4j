package com.weiran.cqt.application.setting;

import com.weiran.cqt.api.setting.SiteConfigService;
import com.weiran.cqt.domain.setting.SiteConfig;
import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** {@link SiteConfigService} 实现：通过基座配置服务读取站点配置，交给领域规则组装。 */
public class SiteConfigApplicationService implements SiteConfigService {

    private static final String KEY_PREFIX = "system.cqt_site.";

    private final ConfigService configService;

    /** 构造服务。 */
    public SiteConfigApplicationService(final ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Map<String, @Nullable Object> getConfig() {
        final Map<Integer, @Nullable String> contents = new HashMap<>();
        for (final ConfigView config : this.configService.all()) {
            if (!config.configKey().startsWith(SiteConfigApplicationService.KEY_PREFIX)) {
                continue;
            }
            final Integer ident = SiteConfig.identForKey(
                    config.configKey().substring(SiteConfigApplicationService.KEY_PREFIX.length()));
            if (ident != null) {
                contents.put(ident, config.configValue().isEmpty() ? null : config.configValue());
            }
        }
        return SiteConfig.build(contents);
    }
}
