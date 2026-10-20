package com.weiran.cqt.application.sms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.weiran.cqt.api.error.CqtErrors;
import com.weiran.cqt.domain.sms.SmsCode;
import com.weiran.cqt.domain.sms.SmsCodeStore;
import com.weiran.cqt.domain.sms.SmsIpCounter;
import com.weiran.framework.error.BizException;
import com.weiran.framework.error.CommonErrors;
import com.weiran.platform.api.config.ConfigService;
import com.weiran.platform.api.config.ConfigView;
import com.weiran.system.api.sms.SmsSendCommand;
import com.weiran.system.api.sms.SmsSendResult;
import com.weiran.system.api.sms.SmsService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmsApplicationServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-04T00:00:00Z"), ZoneOffset.UTC);

    private static final String IP = "203.0.113.9";

    private static final String TEMPLATE_KEY = "system.user.cqt_sms_template_code";

    /** 内存存储。 */
    private static final class MapStore implements SmsCodeStore {

        private final Map<String, SmsCode> codes = new HashMap<>();

        @Override
        public Optional<SmsCode> get(final String phone) {
            return Optional.ofNullable(this.codes.get(phone));
        }

        @Override
        public void put(final String phone, final SmsCode code) {
            this.codes.put(phone, code);
        }

        @Override
        public void remove(final String phone) {
            this.codes.remove(phone);
        }
    }

    /** 内存 IP 计数（不过期）。 */
    private static final class MapCounter implements SmsIpCounter {

        private final Map<String, Integer> counts = new HashMap<>();

        @Override
        public int sentWithinHour(final String ip) {
            return this.counts.getOrDefault(ip, 0);
        }

        @Override
        public void recordSent(final String ip) {
            this.counts.merge(ip, 1, Integer::sum);
        }
    }

    /** 可控的 system 发送服务。 */
    private static final class ScriptedSystemSmsService implements SmsService {

        private final Deque<SmsSendResult> outcomes = new ArrayDeque<>();

        private @Nullable String lastCode;

        private boolean available = true;

        ScriptedSystemSmsService(final SmsSendResult... outcomes) {
            for (final SmsSendResult outcome : outcomes) {
                this.outcomes.addLast(outcome);
            }
        }

        @Override
        public boolean isAvailable() {
            return this.available;
        }

        @Override
        public SmsSendResult send(final SmsSendCommand command) {
            this.lastCode = command.templateParameters().get("code");
            return this.outcomes.isEmpty() ? SmsSendResult.SENT : this.outcomes.pollFirst();
        }
    }

    private static SmsApplicationService service(
            final SmsCodeStore store,
            final SmsIpCounter counter,
            final int ipLimit,
            final ScriptedSystemSmsService sender) {
        final ConfigService configService = mock(ConfigService.class);
        when(configService.all())
                .thenReturn(List.of(new ConfigView(
                        1L,
                        SmsApplicationServiceTest.TEMPLATE_KEY,
                        "TEST_TEMPLATE",
                        "string",
                        "user",
                        "input",
                        null,
                        false,
                        null,
                        true,
                        null,
                        null)));
        return new SmsApplicationService(
                store, configService, sender, counter, SmsApplicationServiceTest.CLOCK, ipLimit);
    }

    private static void assertError(final Runnable action, final Object errorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BizException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(errorCode));
    }

    @Test
    @DisplayName("系统短信不可用时返回未配置错误，且不生成验证码")
    void notConfigured() {
        final MapStore store = new MapStore();
        final ScriptedSystemSmsService sender = new ScriptedSystemSmsService();
        sender.available = false;

        SmsApplicationServiceTest.assertError(
                () -> SmsApplicationServiceTest.service(store, new MapCounter(), 10, sender)
                        .send("15533716215", IP),
                CqtErrors.SMS_NOT_CONFIGURED);
        assertThat(store.codes).isEmpty();
    }

    @Test
    @DisplayName("使用系统模板发送验证码；响应不回显，验证码校验成功后作废")
    void sendsWithSystemTemplateAndVerifiesOnce() {
        final MapStore store = new MapStore();
        final ScriptedSystemSmsService sender = new ScriptedSystemSmsService(SmsSendResult.LOGGED);
        final SmsApplicationService service = SmsApplicationServiceTest.service(store, new MapCounter(), 10, sender);

        assertThat(service.send("15533716215", IP).code()).isNull();
        final String code = String.valueOf(sender.lastCode);
        assertThat(code).matches("\\d{6}");
        service.verify("15533716215", code);
        assertThat(store.codes).isEmpty();
        SmsApplicationServiceTest.assertError(() -> service.verify("15533716215", code), CqtErrors.SMS_CODE_INVALID);
    }

    @Test
    @DisplayName("系统发送失败后回滚验证码、不占冷却或 IP 额度；随后可重发")
    void failureRollsBack() {
        final MapStore store = new MapStore();
        final MapCounter counter = new MapCounter();
        final ScriptedSystemSmsService sender = new ScriptedSystemSmsService(SmsSendResult.FAILED, SmsSendResult.SENT);
        final SmsApplicationService service = SmsApplicationServiceTest.service(store, counter, 10, sender);

        SmsApplicationServiceTest.assertError(() -> service.send("15533716215", IP), CqtErrors.SMS_SEND_FAILED);
        final String failedCode = String.valueOf(sender.lastCode);
        assertThat(store.codes).isEmpty();
        assertThat(counter.sentWithinHour(IP)).isZero();

        service.send("15533716215", IP);
        assertThat(counter.sentWithinHour(IP)).isEqualTo(1);
        if (!failedCode.equals(sender.lastCode)) {
            SmsApplicationServiceTest.assertError(
                    () -> service.verify("15533716215", failedCode), CqtErrors.SMS_CODE_INVALID);
        }
    }

    @Test
    @DisplayName("系统短信供应商错误映射为前台频控或手机号格式错误")
    void mapsProviderOutcomes() {
        final SmsApplicationService service = SmsApplicationServiceTest.service(
                new MapStore(),
                new MapCounter(),
                10,
                new ScriptedSystemSmsService(SmsSendResult.RATE_LIMITED, SmsSendResult.INVALID_NUMBER));

        SmsApplicationServiceTest.assertError(() -> service.send("15533716215", IP), CqtErrors.SMS_TOO_FREQUENT);
        assertThatThrownBy(() -> service.send("15533716215", IP)).isInstanceOfSatisfying(BizException.class, ex -> {
            assertThat(ex.getErrorCode()).isEqualTo(CommonErrors.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("手机号格式不正确");
        });
    }
}
