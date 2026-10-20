package com.weiran.system.application.auth;

import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import com.weiran.system.api.sms.SmsService;
import java.util.Map;
import java.util.stream.Collectors;

/** 手机号验证码登录的动态设置。 */
public class PhoneLoginSettings {

    private static final String ENABLED_KEY = "system.user.enable_phone_login";

    private static final String TEMPLATE_KEY = "system.user.phone_login_sms_template_code";

    private final ConfigService configService;

    private final SmsService smsService;

    public PhoneLoginSettings(final ConfigService configService, final SmsService smsService) {
        this.configService = configService;
        this.smsService = smsService;
    }

    /** 管理开关、发送器与短信模板均就绪时才开放登录。 */
    public boolean isEnabled() {
        final Map<String, String> values = this.values();
        return this.smsService.isAvailable()
                && Boolean.parseBoolean(values.getOrDefault(PhoneLoginSettings.ENABLED_KEY, "false"))
                && !values.getOrDefault(PhoneLoginSettings.TEMPLATE_KEY, "").isBlank();
    }

    /** 已配置的验证码短信模板 CODE。 */
    public String templateCode() {
        return this.values().getOrDefault(PhoneLoginSettings.TEMPLATE_KEY, "").strip();
    }

    private Map<String, String> values() {
        return this.configService.all().stream()
                .filter(config -> config.configKey().equals(PhoneLoginSettings.ENABLED_KEY)
                        || config.configKey().equals(PhoneLoginSettings.TEMPLATE_KEY))
                .collect(Collectors.toMap(ConfigView::configKey, ConfigView::configValue, (first, ignored) -> first));
    }
}
