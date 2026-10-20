package com.weiran.system.infrastructure.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** system 短信发送配置。 */
@ConfigurationProperties("weiran.system.sms")
public record SystemSmsProperties(
        String mode,
        String accessKeyId,
        String accessKeySecret,
        String signName,
        String connectTimeout,
        String readTimeout) {}
