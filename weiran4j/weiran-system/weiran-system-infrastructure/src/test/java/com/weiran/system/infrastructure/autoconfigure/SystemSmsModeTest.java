package com.weiran.system.infrastructure.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.weiran.system.domain.sms.SmsSender;
import com.weiran.system.infrastructure.sms.DisabledSmsSender;
import com.weiran.system.infrastructure.sms.LogSmsSender;
import com.weiran.system.infrastructure.sms.SystemSmsProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemSmsModeTest {

    private final SystemInfrastructureAutoConfiguration configuration = new SystemInfrastructureAutoConfiguration();

    @Test
    @DisplayName("disabled 模式保持默认关闭")
    void createsDisabledSender() {
        final SystemSmsProperties properties = new SystemSmsProperties("disabled", "", "", "", "3", "5");

        final SmsSender sender = this.configuration.systemSmsSender(properties, new ObjectMapper());

        assertThat(sender).isInstanceOf(DisabledSmsSender.class);
    }

    @Test
    @DisplayName("log 模式装配日志 sender 且不要求阿里云凭据")
    void createsLogSenderWithoutCloudCredentials() {
        final SystemSmsProperties properties = new SystemSmsProperties("log", "", "", "", "3", "5");

        final SmsSender sender = this.configuration.systemSmsSender(properties, new ObjectMapper());

        assertThat(sender).isInstanceOf(LogSmsSender.class);
    }
}
