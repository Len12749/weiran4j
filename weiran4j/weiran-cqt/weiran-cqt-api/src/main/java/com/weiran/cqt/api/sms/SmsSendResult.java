package com.weiran.cqt.api.sms;

import org.jspecify.annotations.Nullable;

/**
 * 发送结果。
 *
 * @param sent 是否已发送
 * @param code 保留字段；业务接口不回显验证码，始终为空
 */
public record SmsSendResult(boolean sent, @Nullable String code) {}
