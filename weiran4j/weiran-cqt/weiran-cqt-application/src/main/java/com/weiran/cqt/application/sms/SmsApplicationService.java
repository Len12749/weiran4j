package com.weiran.cqt.application.sms;

import com.weiran.cqt.api.error.CqtErrors;
import com.weiran.cqt.api.sms.SmsSendResult;
import com.weiran.cqt.api.sms.SmsService;
import com.weiran.cqt.domain.account.Phones;
import com.weiran.cqt.domain.sms.SmsCodePolicy;
import com.weiran.cqt.domain.sms.SmsCodeStore;
import com.weiran.framework.error.BizException;
import com.weiran.framework.error.CommonErrors;
import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;

/**
 * {@link SmsService} 实现。发送与校验都是「读 → 判断 → 写」，按实例串行化，避免并发请求绕过冷却、IP 上限或让同一验证码用两次
 * （单实例部署；多实例时连同存储、IP 计数一起换成共享实现）。
 *
 * <p>发送失败时删除刚生成的验证码：用户没收到短信，不该被 60 秒冷却挡住；失败也不计入 IP 上限（服务商只对成功发送计费）。
 */
@Slf4j
public class SmsApplicationService implements SmsService {

    private static final int CODE_BOUND = 1_000_000;

    private static final String LOCAL_TEMPLATE_CONFIG_KEY = "sms.system_tpl.captcha_local";

    private static final String ALIYUN_TEMPLATE_CONFIG_KEY = "sms.system_tpl.captcha_aliyun";

    private final SmsCodeStore codeStore;

    private final ConfigService configService;

    private final com.weiran.system.api.sms.SmsService systemSmsService;

    private final Clock clock;

    private final String mode;

    private final SecureRandom random = new SecureRandom();

    /**
     * 构造服务；短信模板与发送方式均使用 system 配置。
     */
    public SmsApplicationService(
            final SmsCodeStore codeStore,
            final ConfigService configService,
            final com.weiran.system.api.sms.SmsService systemSmsService,
            final Clock clock,
            final @Value("${weiran.sms.mode:disabled}") String mode) {
        this.codeStore = codeStore;
        this.configService = configService;
        this.systemSmsService = systemSmsService;
        this.clock = clock;
        this.mode = mode;
    }

    @Override
    public synchronized SmsSendResult send(final @Nullable String rawPhone) {
        final String phone = Phones.require(rawPhone);
        if (!this.systemSmsService.isAvailable()) {
            throw new BizException(CqtErrors.SMS_NOT_CONFIGURED);
        }
        final String templateCode = this.configService.all().stream()
                .filter(config -> this.templateConfigKey().equals(config.configKey()))
                .map(ConfigView::configValue)
                .filter(value -> !value.isBlank())
                .filter(value -> !"local".equals(this.mode) || value.contains("${code}"))
                .findFirst()
                .orElseThrow(() -> new BizException(CqtErrors.SMS_NOT_CONFIGURED));
        final Instant now = this.clock.instant();
        if (SmsCodePolicy.inCooldown(this.codeStore.get(phone).orElse(null), now)) {
            throw new BizException(CqtErrors.SMS_TOO_FREQUENT);
        }
        final String code = String.format(Locale.ROOT, "%06d", this.random.nextInt(SmsApplicationService.CODE_BOUND));
        this.codeStore.put(phone, SmsCodePolicy.issue(code, now));
        final com.weiran.system.api.sms.SmsSendResult outcome = this.systemSmsService.send(
                new com.weiran.system.api.sms.SmsSendCommand(phone, templateCode, java.util.Map.of("code", code)));
        if (outcome != com.weiran.system.api.sms.SmsSendResult.SENT
                && outcome != com.weiran.system.api.sms.SmsSendResult.LOGGED) {
            this.codeStore.remove(phone);
            throw switch (outcome) {
                case RATE_LIMITED -> new BizException(CqtErrors.SMS_TOO_FREQUENT);
                case INVALID_NUMBER -> new BizException(CommonErrors.BAD_REQUEST, "手机号格式不正确");
                case NOT_CONFIGURED -> new BizException(CqtErrors.SMS_NOT_CONFIGURED);
                default -> new BizException(CqtErrors.SMS_SEND_FAILED);
            };
        }
        SmsApplicationService.log.info("已发送短信验证码 phone={}", Phones.mask(phone));
        return new SmsSendResult(true, null);
    }

    private String templateConfigKey() {
        return "aliyun".equals(this.mode)
                ? SmsApplicationService.ALIYUN_TEMPLATE_CONFIG_KEY
                : SmsApplicationService.LOCAL_TEMPLATE_CONFIG_KEY;
    }

    @Override
    public synchronized void verify(final String phone, final @Nullable String code) {
        final SmsCodePolicy.Verification verification =
                SmsCodePolicy.verify(this.codeStore.get(phone).orElse(null), code, this.clock.instant());
        if (verification.remaining() == null) {
            this.codeStore.remove(phone);
        } else {
            this.codeStore.put(phone, verification.remaining());
        }
        if (!verification.matched()) {
            throw new BizException(CqtErrors.SMS_CODE_INVALID);
        }
    }
}
